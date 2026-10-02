"""Concept redesign of drone tiers 4-9 to match the Drone Tower (proposal stage).

Justin kept tiers 1-3 as they are and asked for tiers 4-9 to be recreated in the tower's theme:
dystopian, militaristic, real rather than default sci-fi. So these are dark, matte, armoured military
drones; every glow is the tower's dull red (status and navigation lights, emitters, coil rings) and there
is no cyan, blue or purple. Each hull is in the material of the tower tier that unlocks it:
  4 aluminium cladding   5 tungsten-steel frame   6 carbon composite
  7 silicon carbide      8 depleted uranium       9 graphene lattice + YBCO conduit
Shapes keep each drone's role and size class (medium fits a 6x4x4 hangar, large is at most one 5x5 pad)
and its stats are unchanged.

`python tools/drone_concepts.py` writes build/drones/drone_concepts.png (tiers 1-3 as they are in game,
4-9 redesigned) using a concept texture sheet generated here.
"""
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import drone_portraits as dp
import drones

R = 32  # pixels per texture region
MATS = {}  # name -> region


def _atlas():
    img = Image.new("RGBA", (8 * R, 8 * R), (0, 0, 0, 0))
    rng = np.random.default_rng(7)
    slots = iter([(c, r) for r in range(8) for c in range(8)])

    def panel(name, base, seams=True, noise=6, stripes=None):
        c, r = next(slots)
        x0, y0 = c * R, r * R
        arr = np.zeros((R, R, 4), np.uint8)
        n = rng.integers(-noise, noise + 1, (R, R, 1)) if noise else 0
        base = tuple(min(255, int(v * 1.3)) for v in base) if seams or noise > 3 else base
        arr[..., :3] = np.clip(np.array(base) + n, 0, 255)
        arr[..., 3] = 255
        if seams:
            dark = np.clip(np.array(base) * 0.62, 0, 255)
            arr[0, :, :3] = arr[:, 0, :3] = dark
            arr[R // 2, 4:R - 4, :3] = dark * 1.15
            for (px, py) in ((3, 3), (R - 4, 3), (3, R - 4), (R - 4, R - 4)):
                arr[py, px, :3] = np.clip(np.array(base) * 1.25, 0, 255)
        if stripes:
            for y in range(R):
                for x in range(R):
                    if ((x + y) // 4) % 2:
                        arr[y, x, :3] = stripes
        img.paste(Image.fromarray(arr, "RGBA"), (x0, y0))
        MATS[name] = (x0, y0, x0 + R, y0 + R)

    def blades(name, count, colour=(78, 80, 86)):
        c, r = next(slots)
        x0, y0 = c * R, r * R
        tile = Image.new("RGBA", (R, R), (0, 0, 0, 0))
        d = ImageDraw.Draw(tile)
        cx = cy = R / 2
        for k in range(count):
            a = 2 * math.pi * k / count
            tip = (cx + math.cos(a) * R * 0.49, cy + math.sin(a) * R * 0.49)
            side = (math.cos(a + math.pi / 2) * 1.8, math.sin(a + math.pi / 2) * 1.8)
            d.polygon([(cx + side[0], cy + side[1]), (tip[0] + side[0] * 0.6, tip[1] + side[1] * 0.6),
                       (tip[0] - side[0] * 0.6, tip[1] - side[1] * 0.6), (cx - side[0], cy - side[1])], fill=colour + (255,))
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(60, 62, 66, 255))
        img.paste(tile, (x0, y0))
        MATS[name] = (x0, y0, x0 + R, y0 + R)

    panel("alu", (118, 122, 128))        # tier 4: aluminium cladding, gunmetal-painted
    panel("alu_dark", (78, 82, 88))
    panel("tungsten", (66, 68, 74))      # tier 5
    panel("tungsten_dark", (46, 48, 54))
    panel("carbon", (40, 42, 46), noise=3)  # tier 6
    panel("carbon_dark", (28, 29, 32), noise=3)
    panel("sic", (60, 66, 76))           # tier 7
    panel("sic_dark", (42, 46, 54))
    panel("du", (54, 60, 52))            # tier 8
    panel("du_dark", (38, 42, 37))
    panel("graphene", (32, 34, 38), noise=3)  # tier 9
    panel("graphene_dark", (22, 23, 26), noise=2)
    panel("steel", (88, 92, 98), seams=False)
    panel("black", (20, 20, 22), seams=False, noise=2)
    panel("glass", (36, 30, 32), seams=False, noise=2)
    panel("sensor", (16, 16, 18), seams=False, noise=1)
    panel("vent", (34, 36, 40), seams=False, stripes=(20, 20, 22))
    panel("hazard", (214, 170, 38), seams=False, noise=3, stripes=(26, 26, 28))
    panel("glow", (255, 255, 255), seams=False, noise=0)
    panel("conduit", (120, 28, 26), seams=False, noise=4)
    blades("blades2", 2)
    blades("blades3", 3)
    blades("blades4", 4)
    blades("blades5", 5)
    blades("blades6", 6, (52, 54, 58))
    panel("gunmetal", (70, 74, 80))       # tiers 1-3: painted steel
    panel("gunmetal_dark", (46, 48, 54))
    panel("armor_plate", (60, 64, 70))
    panel("frame", (34, 35, 38), seams=False, noise=2)
    return np.asarray(img, dtype=np.float32) / 255.0


TEX = _atlas()
M = MATS
DULL_RED, WARN_RED = 0xFF96261E, 0xFFEB3E2C


def light(g, x, y, z, s=0.07, colour=WARN_RED):
    dp.glow(g, colour, lambda: g.centred(x, y, z, s, s, s, M["glow"]))


def duct(g, x, y, z, r, h, wall, mat):
    g.box(x - r, y, z - r, x + r, y + h, z - r + wall, mat)
    g.box(x - r, y, z + r - wall, x + r, y + h, z + r, mat)
    g.box(x - r, y, z - r, x - r + wall, y + h, z + r, mat)
    g.box(x + r - wall, y, z - r, x + r, y + h, z + r, mat)


# ---------------------------------------------------------------- tier 4: Ducted-Fan Runner

def runner(g):
    """Low armoured quad-duct runner: wedge hull, four shrouded fans on stub pylons, skids, chin sensor."""
    hull, dark = M["alu"], M["alu_dark"]
    g.centred(0, 0.30, 0, 0.62, 0.22, 1.0, hull)
    g.centred(0, 0.44, -0.08, 0.46, 0.08, 0.7, dark)
    g.centred(0, 0.27, 0.57, 0.42, 0.14, 0.16, dark)
    g.centred(0, 0.24, 0.68, 0.24, 0.08, 0.08, dark)
    g.centred(0, 0.14, 0.46, 0.13, 0.11, 0.13, M["sensor"])
    g.centred(0, 0.49, -0.42, 0.03, 0.22, 0.03, M["steel"])  # antenna
    for sx in (-1, 1):
        g.box(sx * 0.26 - 0.025, 0, -0.38, sx * 0.26 + 0.025, 0.04, 0.38, M["black"])
        for sz in (-0.25, 0.25):
            g.centred(sx * 0.26, 0.12, sz, 0.035, 0.18, 0.035, M["black"])
        for sz in (-1, 1):
            fx, fz = sx * 0.66, sz * 0.5
            g.box(min(sx * 0.3, fx), 0.3, fz - 0.05, max(sx * 0.3, fx), 0.36, fz + 0.05, dark)
            duct(g, fx, 0.22, fz, 0.29, 0.2, 0.05, dark)
            dp.rotor(g, fx, 0.31, fz, 0.25, sx * sz * 0.6, M["blades4"])
            g.centred(fx, 0.31, fz, 0.08, 0.04, 0.08, M["steel"])
        light(g, sx * 0.95, 0.36, 0.5, 0.05, DULL_RED)
    light(g, 0, 0.6, -0.42, 0.05)


# ---------------------------------------------------------------- tier 5: Tiltrotor Carrier

def tiltrotor(g):
    """Unmanned tiltrotor transport: faceted fuselage, high wing, tilting nacelles with 3-blade rotors,
    V-tail, chin sensor turret, cargo door underneath."""
    hull, dark = M["tungsten"], M["tungsten_dark"]
    g.centred(0, 0.36, 0, 0.44, 0.36, 1.6, hull)
    g.centred(0, 0.58, 0.05, 0.3, 0.1, 1.1, dark)
    g.centred(0, 0.33, 0.87, 0.34, 0.26, 0.16, hull)
    g.centred(0, 0.31, 1.0, 0.22, 0.18, 0.12, dark)
    g.centred(0, 0.17, 0.82, 0.12, 0.1, 0.12, M["sensor"])
    g.centred(0, 0.18, -0.2, 0.3, 0.02, 0.6, M["hazard"])  # cargo door outline
    g.centred(0, 0.4, -0.95, 0.3, 0.24, 0.32, hull)
    g.centred(0, 0.64, 0.12, 2.3, 0.06, 0.4, dark)
    for side in (-1, 1):
        g.push()
        g.translate(side * 0.12, 0.5, -0.98).roll(-side * 0.6)
        g.centred(0, 0.2, 0, 0.04, 0.42, 0.3, dark)
        g.pop()
        g.centred(side * 1.18, 0.66, 0.12, 0.2, 0.2, 0.55, hull)
        g.centred(side * 1.18, 0.66, 0.42, 0.14, 0.14, 0.08, dark)
        dp.rotor(g, side * 1.18, 0.82, 0.18, 0.6, side * 0.3, M["blades3"])
        g.centred(side * 1.18, 0.8, 0.18, 0.08, 0.06, 0.08, M["steel"])
        g.centred(side * 0.18, 0.08, 0.5, 0.05, 0.16, 0.05, M["black"])
        light(g, side * 1.18, 0.66, -0.17, 0.05, DULL_RED)
    g.centred(0, 0.08, -0.55, 0.05, 0.16, 0.05, M["black"])
    light(g, 0, 0.74, -1.08, 0.05)


# ---------------------------------------------------------------- tier 6: Tandem Freighter

def tandem(g):
    """Heavy tandem-rotor freighter: boxy carbon-composite hull, sponsons, rear loading ramp with hazard
    edge, front and rear rotor pylons with 4-blade rotors, sensor ball."""
    hull, dark = M["carbon"], M["carbon_dark"]
    g.centred(0, 0.5, 0, 0.72, 0.6, 2.2, hull)
    g.centred(0, 0.86, -0.05, 0.44, 0.12, 1.7, dark)
    g.centred(0, 0.44, 1.16, 0.62, 0.46, 0.14, hull)
    g.centred(0, 0.4, 1.26, 0.46, 0.32, 0.08, dark)
    g.centred(0, 0.2, 1.08, 0.14, 0.12, 0.14, M["sensor"])
    g.centred(0, 0.32, -1.13, 0.62, 0.36, 0.06, dark)
    g.centred(0, 0.15, -1.14, 0.62, 0.04, 0.07, M["hazard"])
    g.centred(0, 0.98, 0.92, 0.32, 0.26, 0.36, dark)
    g.centred(0, 1.1, -0.92, 0.42, 0.52, 0.5, hull)
    g.centred(0, 1.0, -0.6, 0.3, 0.2, 0.3, M["vent"])
    for sx in (-1, 1):
        g.centred(sx * 0.42, 0.3, 0.15, 0.14, 0.24, 1.1, dark)
        for wz in (0.75, -0.7):
            g.centred(sx * 0.42, 0.08, wz, 0.08, 0.16, 0.16, M["black"])
        light(g, sx * 0.5, 0.42, 0.72, 0.05, DULL_RED)
    dp.rotor(g, 0, 1.13, 0.92, 0.84, 0.2, M["blades4"])
    dp.rotor(g, 0, 1.38, -0.92, 0.84, 0.6, M["blades4"])
    g.centred(0, 1.12, 0.92, 0.1, 0.06, 0.1, M["steel"])
    g.centred(0, 1.37, -0.92, 0.1, 0.06, 0.1, M["steel"])
    light(g, 0, 1.4, -0.6, 0.06)


# ---------------------------------------------------------------- tier 7: Hybrid Aerostat

def aerostat(g):
    """Armoured hybrid airship: a flattened, tri-lobed lifting-body hull in silicon-carbide plates with
    armour bands, X tail, four vectored ducted props on pylons, a gondola with a sensor turret, dim red
    strip lights along its flanks."""
    hull, dark = M["sic"], M["sic_dark"]
    g.centred(0, 1.45, 0, 2.3, 0.86, 3.7, hull)
    g.centred(0, 1.45, 0, 1.7, 1.25, 4.0, hull)
    g.centred(0, 1.5, 0.05, 1.1, 1.5, 4.2, dark)
    for z in (-1.2, 0.0, 1.2):
        g.centred(0, 1.45, z, 2.34, 0.9, 0.12, dark)
        g.centred(0, 1.45, z, 1.74, 1.29, 0.12, dark)
    for sx in (-1, 1):
        dp.glow(g, DULL_RED, lambda: g.centred(sx * 1.16, 1.45, 0, 0.02, 0.05, 2.8, M["glow"]))
    for roll in (0.75, -0.75, 2.39, -2.39):
        g.push()
        g.translate(0, 1.45, -1.75).roll(roll)
        g.centred(0, 0.9, 0, 0.07, 0.75, 0.62, dark)
        g.pop()
    g.centred(0, 0.62, 0.25, 0.62, 0.4, 1.4, dark)
    g.centred(0, 0.36, 0.7, 0.18, 0.14, 0.18, M["sensor"])
    g.centred(0, 0.62, 0.97, 0.46, 0.28, 0.06, M["glass"])
    g.box(-0.04, 0.8, -0.3, 0.04, 1.05, 0.7, M["steel"])
    for sx in (-1, 1):
        for sz in (-1, 1):
            px, pz = sx * 1.55, sz * 1.15
            g.box(min(sx * 1.0, px), 1.05, pz - 0.05, max(sx * 1.0, px), 1.12, pz + 0.05, M["steel"])
            duct(g, px, 0.92, pz, 0.32, 0.3, 0.06, dark)
            dp.rotor(g, px, 1.06, pz, 0.28, sx * sz * 0.5, M["blades5"])
            light(g, px + sx * 0.33, 1.07, pz, 0.05, DULL_RED)
    g.box(-0.3, 0, -0.3, 0.3, 0.42, 0.3, M["black"])
    light(g, 0, 2.27, 0.2, 0.08)


# ---------------------------------------------------------------- tier 8: Ion-Wind Glider

def glider(g):
    """Stealth flying wing in depleted-uranium armour: faceted centre body, swept sawtooth wings, flush
    lift-fan grilles instead of open ducts, dim red ion emitter strips along the leading edges, small
    canted winglets, retractable-style gear."""
    hull, dark = M["du"], M["du_dark"]
    g.centred(0, 0.42, 0.25, 1.0, 0.3, 2.0, hull)
    g.centred(0, 0.6, 0.35, 0.6, 0.1, 1.2, dark)
    g.centred(0, 0.4, 1.3, 0.6, 0.2, 0.14, hull)
    g.centred(0, 0.38, 1.42, 0.3, 0.14, 0.1, dark)
    g.centred(0, 0.22, 1.05, 0.12, 0.1, 0.12, M["sensor"])
    for side in (-1, 1):
        for i in range(4):
            cx = side * (0.72 + i * 0.45)
            cz = 0.2 - i * 0.24
            chord = 1.5 - i * 0.24
            g.centred(cx, 0.44, cz, 0.47, 0.08, chord, hull)
            lead = cz + chord / 2
            dp.glow(g, DULL_RED, lambda: g.centred(cx, 0.47, lead - 0.03, 0.46, 0.03, 0.04, M["glow"]))
            g.centred(cx, 0.44, cz - chord / 2 + 0.05, 0.47, 0.09, 0.1, dark)
        for fx, fz in ((1.0, 0.1), (1.8, -0.25)):
            g.centred(side * fx, 0.485, fz, 0.42, 0.01, 0.42, M["vent"])
        g.push()
        g.translate(side * 2.2, 0.46, -0.55).roll(-side * 0.5)
        g.centred(0, 0.17, 0, 0.05, 0.34, 0.4, dark)
        g.pop()
        light(g, side * 2.22, 0.48, -0.32, 0.05)
        g.box(side * 0.47 - 0.03, 0, 0.1, side * 0.47 + 0.03, 0.28, 0.16, M["black"])
    g.box(-0.03, 0, 0.9, 0.03, 0.28, 0.96, M["black"])


# ---------------------------------------------------------------- tier 9: Superconducting Heavy Lifter

def multirotor(g, arms, scale, hull, dark, accent=None, dome=False):
    """Tiers 1-3, same shapes as in game (4, 6 and 8 arms), recoloured to the tower's theme: gunmetal or
    armour-plate body, black arms, dark grey blades, a dull red status light instead of the cyan one,
    and a thin hazard or plate accent instead of the bright stripes."""
    g.push()
    g.scale(scale)
    g.centred(0, 0.115, 0, 0.30, 0.11, 0.38, hull)
    g.centred(0, 0.175, -0.02, 0.22, 0.02, 0.26, dark)
    if accent:
        g.centred(0, 0.115, 0.0, 0.305, 0.02, 0.385, accent)
    g.box(-0.13, 0, -0.13, -0.10, 0.06, 0.13, M["frame"])
    g.box(0.10, 0, -0.13, 0.13, 0.06, 0.13, M["frame"])
    if dome:
        g.box(-0.06, 0.02, 0.02, 0.06, 0.07, 0.14, M["sensor"])
    g.centred(0, 0.09, 0.18, 0.07, 0.06, 0.05, M["sensor"])
    dp.glow(g, DULL_RED, lambda: g.box(-0.03, 0.12, 0.19, 0.03, 0.15, 0.205, M["glow"]))
    reach = 0.36 if arms == 8 else 0.34
    blade = 0.13 if arms == 8 else 0.15
    for k in range(arms):
        angle = 2 * math.pi * k / arms + (math.pi / 4 if arms == 4 else 0)
        g.push()
        g.yaw(angle)
        g.box(-0.018, 0.118, 0, 0.018, 0.142, reach, M["frame"])
        g.box(-0.03, 0.11, reach - 0.03, 0.03, 0.165, reach + 0.03, dark)
        dp.rotor(g, 0, 0.17, reach, blade, k, M["blades2"])
        g.pop()
    g.pop()


def courier(g):
    multirotor(g, 4, 1.0, M["gunmetal"], M["gunmetal_dark"])


def hexacopter(g):
    multirotor(g, 6, 1.0, M["gunmetal_dark"], M["gunmetal"], accent=M["hazard"], dome=True)


def octocopter(g):
    multirotor(g, 8, 1.15, M["armor_plate"], M["gunmetal_dark"], accent=M["gunmetal_dark"])


def heavy_lifter(g):
    """Tier 9, remade: a superconducting sky-crane. A long graphene spine with the sensor and control pod
    at the front and the power block at the back, one coaxial mast with two counter-rotating 6-blade
    rotors driven by a superconducting motor (its YBCO coil ring glows dull red), four long splayed legs,
    and a big cargo grab hanging under the spine between them. Nothing like the quad-fan tier 4."""
    hull, dark = M["graphene"], M["graphene_dark"]
    g.centred(0, 1.3, 0, 0.5, 0.42, 3.6, hull)                   # spine
    g.centred(0, 1.52, -0.1, 0.32, 0.1, 2.8, dark)
    g.centred(0, 1.12, 1.85, 0.86, 0.74, 0.7, hull)              # front pod
    g.centred(0, 1.05, 2.25, 0.6, 0.5, 0.14, dark)
    g.centred(0, 0.72, 1.95, 0.22, 0.2, 0.22, M["sensor"])
    g.centred(0, 1.38, -1.6, 0.8, 0.66, 1.0, dark)               # power block
    g.centred(0.41, 1.38, -1.6, 0.01, 0.4, 0.7, M["vent"])
    g.centred(-0.41, 1.38, -1.6, 0.01, 0.4, 0.7, M["vent"])
    g.centred(0, 1.85, -2.05, 0.06, 0.5, 0.42, dark)             # stabiliser
    g.centred(0, 1.75, 0.1, 0.42, 0.5, 0.42, dark)               # mast and motor
    dp.glow(g, DULL_RED, lambda: g.centred(0, 1.98, 0.1, 0.62, 0.08, 0.62, M["conduit"]))
    g.centred(0, 2.12, 0.1, 0.22, 0.36, 0.22, M["steel"])
    dp.rotor(g, 0, 2.1, 0.1, 2.25, 0.0, M["blades6"])
    dp.rotor(g, 0, 2.34, 0.1, 2.2, 0.26, M["blades6"])
    g.centred(0, 2.36, 0.1, 0.14, 0.06, 0.14, M["frame"])
    light(g, 0, 2.44, 0.1, 0.08)
    for sx in (-1, 1):
        for sz in (-1, 1):                                       # splayed legs
            g.push()
            g.translate(sx * 0.22, 1.12, sz * 1.25).roll(sx * 0.55)
            g.centred(0, -0.62, 0, 0.1, 1.3, 0.1, hull)
            g.pop()
            g.centred(sx * 0.82, 0.03, sz * 1.25, 0.3, 0.06, 0.3, M["frame"])
            light(g, sx * 0.62, 0.42, sz * 1.25, 0.05, DULL_RED)
    for x in (-0.14, 0.14):                                      # cables and cargo grab
        g.centred(x, 0.98, 0.1, 0.03, 0.22, 0.03, M["steel"])
    g.centred(0, 0.84, 0.1, 1.5, 0.1, 0.24, M["steel"])
    g.centred(0, 0.84, 0.1, 0.24, 0.1, 1.5, M["steel"])
    for gx, gz in ((-0.7, 0), (0.7, 0), (0, -0.7), (0, 0.7)):
        g.centred(gx, 0.6, 0.1 + gz, 0.08, 0.4, 0.08, dark)
        g.centred(gx, 0.38, 0.1 + gz, 0.14, 0.08, 0.14, M["hazard"])


NEW = {1: courier, 2: hexacopter, 3: octocopter, 4: runner, 5: tiltrotor, 6: tandem, 7: aerostat, 8: glider, 9: heavy_lifter}


def model(tier):
    if tier in NEW:
        g = dp.Geometry()
        NEW[tier](g)
        return g.quads, TEX
    return dp.drone(tier), None


def main():
    out = dp.ROOT / "build" / "drones"
    out.mkdir(parents=True, exist_ok=True)
    F = "/usr/share/fonts/truetype/dejavu/"
    big = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 36)
    mid = ImageFont.truetype(F + "DejaVuSans-Bold.ttf", 22)
    sm = ImageFont.truetype(F + "DejaVuSans.ttf", 17)
    cell, cols = 560, 3
    sheet = Image.new("RGB", (cell * cols + 40, 130 + 3 * (cell + 130)), (16, 12, 14))
    d = ImageDraw.Draw(sheet)
    d.text((20, 20), "DRONES — ALL 9 TIERS IN THE TOWER'S THEME (concept v2)", font=big, fill=(220, 80, 64))
    d.text((20, 68), "Tiers 1-3: same shapes, recoloured. 4-8: redesigned. 9: remade as a sky-crane. Dark matte military hulls, "
                     "dull red lights only, real-world drone layouts.", font=sm, fill=(160, 140, 140))
    d.text((20, 92), "Each drone is scaled to fit its frame (real sizes below); stats are unchanged.", font=sm, fill=(160, 140, 140))
    hull = {1: "gunmetal-painted steel", 2: "dark steel, hazard band", 3: "steel armour plate", 4: "aluminium cladding", 5: "tungsten-steel frame", 6: "carbon composite", 7: "silicon carbide armour",
            8: "depleted-uranium armour", 9: "graphene lattice, YBCO coils"}
    sizes = {1: "~0.8 across", 2: "~0.8 across", 3: "~1 across", 4: "1.9 across", 5: "3.6 across the rotors",
             6: "3.5 long with rotors", 7: "4.2 long", 8: "4.6 wingspan", 9: "4.5 across the rotors, 4.3 long"}
    for tier in range(1, 10):
        quads, tex = model(tier)
        img = dp.render(quads, texture=tex, pitch_deg=12 if tier == 9 else 28)
        info = drones.DRONE_TIERS[tier]
        x = 20 + ((tier - 1) % cols) * cell
        y = 130 + ((tier - 1) // cols) * (cell + 130)
        tile = img.resize((cell - 20, cell - 20), Image.LANCZOS)
        panel = Image.new("RGBA", tile.size, (74, 74, 78, 255))
        panel.alpha_composite(tile)
        panel.convert("RGB").save(out / f"concept_t{tier}.png")
        sheet.paste(panel.convert("RGB"), (x, y))
        d.text((x + 10, y + cell - 10), f"TIER {tier}", font=mid, fill=(220, 80, 64))
        d.text((x + 110, y + cell - 10), info["display"].replace(" Drone", ""), font=mid, fill=(236, 230, 230))
        d.text((x + 10, y + cell + 22), f"{info['size']} · {sizes[tier]} · {info['capacity']} blocks/trip · {info['speed']} blocks/s",
               font=sm, fill=(200, 190, 190))
        d.text((x + 10, y + cell + 44), ("RECOLOURED · " if tier < 4 else "REMADE · " if tier == 9 else "NEW LOOK · ") + "hull: " + hull[tier], font=sm,
               fill=(214, 156, 60))
    sheet.save(out / "drone_concepts.png")
    print(out / "drone_concepts.png")


if __name__ == "__main__":
    main()
