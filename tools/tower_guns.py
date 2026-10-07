"""Tower guns (batch 54, docs/features/tower-guns.md): five heavy emplacements built to stand on top of a tower.

The owner asked for "more guns that look very similar to" a heavy mortar on a turntable mount (a picture of third-party
art used only for its mood: a concrete ring plinth, a railed turntable deck, a yellow armoured cradle with round
ports and pipework, and a fat black barrel), in Jugcraft's style: two that take a 3x3 top and three that take 5x5.

- Bastion Mortar (3x3): a compact high-arc mortar; Heavy Shells.
- Bastion Autocannon (3x3): twin quick-firing barrels in a yellow gunhouse; holds fire; Flak Shells.
- Grand Mortar (5x5): the full emplacement, with a stepped plinth, a tall cradle with an elevation gear and the
  biggest barrel; Great Shells, whose burst reaches further.
- Fortress Rifle (5x5): a long-barrelled gun in an armoured gunhouse that throws Heavy Shells fast and flat, twice as
  far as a mortar.
- Triple Battery (5x5): a low turret with three barrels that fire a salvo of three Heavy Shells.

Each is placed from its item onto the top of a tower (or anything else), centred on the block clicked, and needs a
solid top under its whole footprint. It works like the big guns (batch 51, tools/artillery.py): a gunner climbs
aboard, marks a target with a Range Finder or looks where to fire, and presses attack. Every burst is a Blast
(weapons/Blast): it hurts living things and never breaks a block.

Java: artillery/TowerGun and artillery/JugcraftTowerGuns; client/TowerGunRenderer draws the parts exported here
(assets/jugcraft/tower_gun_quads.json). tools/check_mod_data.py checks that the numbers match.
"""
import json
import math

import clean_metal
import item_icons
from steampunk_models import box, cyl
from zeppelin import tiled_quads

MOD = "jugcraft"

# The Great Shell: muzzle speed (blocks a tick), gravity, blast reach (blocks) and centre damage.
GREAT_SPEED = 3.2
GREAT_GRAVITY = 0.05
GREAT_RADIUS = 7.0
GREAT_DAMAGE = 48

# Each gun. size: its footprint (3 or 5 blocks a side). shell: "heavy", "flak" or "great". speed: the shell's muzzle
# speed. cooldown: ticks between shots. traverse: degrees a tick. pitch: elevation limits. high: fires on the high arc.
# auto: keeps firing while attack is held. barrels: each barrel's offset across the gun, in pixels (one shell from
# each). turntable, trunnion: pivots in pixels from the gun's feet (the turntable turns, the barrel elevates about the
# trunnion). muzzle: the barrel's length from the trunnion, in pixels. seats: (x, y, z) in pixels. height: the
# entity's height in blocks. recoil: how far the barrel kicks back, in pixels.
GUNS = {
    "bastion_mortar": dict(
        name="Bastion Mortar", size=3, shell="heavy", speed=3.0, cooldown=70, traverse=3.0, pitch=(45, 85), high=True,
        auto=False, barrels=[0], turntable=(0, 9, 0), trunnion=(0, 27, 2), muzzle=44, health=160,
        seats=[(0, 12, -15), (11, 12, -9)], height=2.4, recoil=5),
    "bastion_autocannon": dict(
        name="Bastion Autocannon", size=3, shell="flak", speed=4.0, cooldown=6, traverse=10.0, pitch=(-10, 85), high=False,
        auto=True, barrels=[-5, 5], turntable=(0, 9, 0), trunnion=(0, 25, 6), muzzle=50, health=120,
        seats=[(0, 12, -16)], height=2.2, recoil=2),
    "grand_mortar": dict(
        name="Grand Mortar", size=5, shell="great", speed=GREAT_SPEED, cooldown=200, traverse=1.0, pitch=(45, 85), high=True,
        auto=False, barrels=[0], turntable=(0, 12, 0), trunnion=(0, 50, 4), muzzle=86, health=320,
        seats=[(0, 15, -26), (20, 15, -16), (-20, 15, -16)], height=4.0, recoil=8),
    "fortress_rifle": dict(
        name="Fortress Rifle", size=5, shell="heavy", speed=4.5, cooldown=120, traverse=1.5, pitch=(-5, 45), high=False,
        auto=False, barrels=[0], turntable=(0, 12, 0), trunnion=(0, 34, 14), muzzle=104, health=300,
        seats=[(0, 15, -30), (18, 15, -26)], height=3.0, recoil=10),
    "triple_battery": dict(
        name="Triple Battery", size=5, shell="heavy", speed=3.0, cooldown=140, traverse=1.5, pitch=(-5, 60), high=False,
        auto=False, barrels=[-11, 0, 11], turntable=(0, 12, 0), trunnion=(0, 32, 14), muzzle=78, health=300,
        seats=[(0, 15, -30), (18, 15, -26)], height=2.8, recoil=7),
}

TOOLTIPS = {
    "bastion_mortar": "A compact heavy mortar for a 3x3 tower top. Climb aboard and press attack to lob a Heavy Shell at "
                      "your marked target, or where you look.",
    "bastion_autocannon": "Twin quick-firing cannon for a 3x3 tower top. Hold attack to fire Flak Shells that burst "
                          "beside anything flying.",
    "grand_mortar": "A colossal mortar for a 5x5 tower top. It lobs Great Shells, whose bursts reach 7 blocks, at your "
                    "marked target or where you look.",
    "fortress_rifle": "A long gun for a 5x5 tower top. It throws Heavy Shells fast and flat, out to about 200 blocks.",
    "triple_battery": "A three-barrelled turret for a 5x5 tower top. Each press fires a salvo of three Heavy Shells.",
    "great_shell": "Ammunition for the Grand Mortar. Its burst hurts creatures up to 7 blocks away but never breaks blocks.",
}

YELLOW, HAZARD, CONCRETE, DECK, OLIVE = "ar_yellow", "dp_hazard", "ar_concrete", "ar_deck", "ar_olive"
SKID, BAND, NUT, COPPER, GRILLE = "dr_skid", "dr_band", "dr_nut", "dr_copper_pipe", "dp_grille"
BRASS, ARMOR, AMBER = "ik_brass", "ar_armor", "dr_amber_on"
SLOTS = "tg_slots"
# The guns' own clean steel (5 October 2026, the owner: "parts of the grand mortar are invisible ... same with the barrels
# on most of the big guns, lots of their textures are conflicting"). The big faces are mapped by world position in
# 16-pixel cells, so a framed panel with corner bolts (dp_gunmetal) turned every barrel and housing into a stack of
# crates. These tile without a visible frame: tg_tube is a near-flat steel for every barrel and muzzle (a tube shows it
# in four orientations, so it has no direction), tg_steel seamless long plates for housings, roofs and brakes, tg_soot
# a flat sooty dark for vents and exhaust mouths. Bores, port covers and hazard signs are decals ("!" faces), each drawn
# whole on its own plate.
TUBE, STEEL, SOOT = "tg_tube", "tg_steel", "tg_soot"
GUNMETAL = STEEL
PORT, WARNING, BORE = "tg_port!", "tg_warning!", "tg_bore!"


def items():
    """Every item id this batch adds: the five guns and the Great Shell."""
    return list(GUNS) + ["great_shell"]


def footprint_radius(gun):
    """Half the footprint's width, in pixels, less a sliver so a gun never pokes past its tower's edge."""
    return GUNS[gun]["size"] * 8 - 0.5


# ------------------------------------------------------------------ shared parts

def plinth(r, tall=False):
    """The fixed concrete ring the gun stands on: a steel rim, anchor feet at the corners and, for the big guns, a
    second stepped ring with vent slots and a yellow band."""
    m = cyl("y", 0, 0, r, 0, 5, CONCRETE, CONCRETE)
    m += cyl("y", 0, 0, r + 0.5, 4, 6, BAND)
    foot = r * 0.72
    for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        x, z = sx * foot, sz * foot
        m.append(box((x - 3, 0, z - 3), (x + 3, 3, z + 3), SKID))
    if tall:
        m += cyl("y", 0, 0, r - 5, 6, 11, SLOTS, CONCRETE)
        m += cyl("y", 0, 0, r - 4.5, 8, 9, YELLOW)
        m += cyl("y", 0, 0, r - 7, 11, 12, SKID, DECK)
    else:
        m += cyl("y", 0, 0, r - 4, 6, 9, SKID, DECK)
    return m


def railing(r, y, height, start, end, step=30):
    """Posts and a top rail round an arc of the deck (angles in degrees, 0 = forward, 180 = back)."""
    m = []
    for angle in range(start, end + 1, step):
        a = math.radians(angle)
        x, z = r * math.sin(a), r * math.cos(a)
        m.append(box((x - 0.5, y, z - 0.5), (x + 0.5, y + height, z + 0.5), SKID))
    for a0 in range(start, end, step):
        a, b = math.radians(a0), math.radians(a0 + step)
        x0, z0, x1, z1 = r * math.sin(a), r * math.cos(a), r * math.sin(b), r * math.cos(b)
        mx, mz = (x0 + x1) / 2, (z0 + z1) / 2
        half = math.hypot(x1 - x0, z1 - z0) / 2
        angle = math.degrees(math.atan2(x1 - x0, z1 - z0))
        snapped = max(-45, min(45, round(((angle + 90) % 180 - 90) / 22.5) * 22.5))
        m.append(box((mx - 0.5, y + height - 1, mz - half), (mx + 0.5, y + height, mz + half), SKID,
                     rotation=("y", snapped, (mx, y + height - 1, mz))))
    return m


def deck(r):
    """The turning deck: tread plate with a hazard-striped edge (the stripes only round its rim, the top is tread)."""
    return cyl("y", 0, 0, r, 0, 2, SKID, DECK) + cyl("y", 0, 0, r + 0.5, 2, 3, HAZARD, DECK)


def shell_rack(x, z, count):
    """A rack of shells standing on the deck."""
    m = []
    for i in range(count):
        m += cyl("y", x + i * 3.5, z, 1.4, 3, 10, BRASS, TUBE)
    return m


def bore(z, r, ring, cx=0.0, cy=0.0):
    """A muzzle's bore: a plate 0.1 pixel proud of the muzzle face at z (facing +z), drawn once with the whole tg_bore
    decal, a dark round bore on barrel steel. The decal's dark disc spans three quarters of the plate, so the bore's
    radius is r, or less where the plate must stay inside three quarters of the muzzle's radius `ring` (where the
    stepped muzzle's face is solid). The plate stands off the muzzle face, so the two never share a plane."""
    s = math.floor(min(r / 0.75, ring * 0.75) * 4) / 4
    return box((cx - s, cy - s, z), (cx + s, cy + s, z + 0.1), {"*": TUBE, "south": BORE})


def port_plate(face, x, y0, y1, z0, z1, proud=0.5):
    """A round port cover (tg_port, drawn whole) on a yellow plate standing `proud` pixels off a wall whose outer face is
    at x, facing east or west."""
    x0, x1 = (x, x + proud) if face == "east" else (x - proud, x)
    return box((x0, y0, z0), (x1, y1, z1), {"*": YELLOW, face: PORT})


def cradle_side(x0, x1, ty, tz, depth, port_r):
    """One yellow cradle cheek rising to the trunnion, with a square port plate (a round X-braced cover, drawn whole)
    over the trunnion on its outer face."""
    m = [box((x0, 3, tz - depth), (x1, ty - 6, tz + depth * 0.7), YELLOW)]
    m.append(box((x0, ty - 6, tz - depth * 0.65), (x1, ty + port_r * 0.6, tz + depth * 0.5), YELLOW))
    m.append(box((x0 - 0.5, 3, tz - depth - 0.5), (x1 + 0.5, 4.5, tz + depth * 0.7 + 0.5), BAND))
    face, wall = ("east", x1) if x0 >= 0 else ("west", x0)
    m.append(port_plate(face, wall, ty - port_r, ty + port_r, tz - port_r, tz + port_r, 1.5))
    return m


def pipe_run(points, r=1.2):
    """Copper pipework along straight runs between points (each run along one axis), with a nut at each bend."""
    m = []
    for (x0, y0, z0), (x1, y1, z1) in zip(points, points[1:]):
        if x0 != x1:
            m += cyl("x", y0, z0, r, min(x0, x1), max(x0, x1), COPPER)
        elif y0 != y1:
            m += cyl("y", x0, z0, r, min(y0, y1), max(y0, y1), COPPER)
        else:
            m += cyl("z", x0, y0, r, min(z0, z1), max(z0, z1), COPPER)
    for x, y, z in points[1:-1]:
        m.append(box((x - r - 0.3, y - r - 0.3, z - r - 0.3), (x + r + 0.3, y + r + 0.3, z + r + 0.3), NUT))
    return m


def fat_breech(breech):
    """A fat barrel's yellow breech housing with its band, about the trunnion: the cradle the barrel recoils through."""
    return [box((-breech, -breech, -breech * 1.5), (breech, breech, breech * 0.3), YELLOW),
            box((-breech - 0.5, -breech - 0.5, -breech * 0.6), (breech + 0.5, breech + 0.5, -breech * 0.6 + 2), BAND)]


def fat_barrel(r, length, breech):
    """A fat black barrel along +z out of its breech (fat_breech): a sleeve over the first stretch, reinforcing bands
    and a heavy muzzle ring round a dark bore. The tube ends inside the muzzle ring, so their faces never share a plane,
    and the sleeve stays at least a quarter pixel inside the breech's sides, which it slides into as the barrel recoils."""
    m = cyl("z", 0, 0, r, breech * 0.3, length - 3, TUBE)
    m += cyl("z", 0, 0, min(r + 1.5, breech - 0.25), breech * 0.3, length * 0.35, TUBE)
    m += cyl("z", 0, 0, r + 1, length * 0.68, length * 0.68 + 3, TUBE)
    m += cyl("z", 0, 0, r + 1.5, length - 6, length, TUBE)
    m.append(bore(length, r * 0.75, r + 1.5))
    return m


# ------------------------------------------------------------------ the Bastion Mortar (3x3)

def bastion_mortar_base():
    return plinth(footprint_radius("bastion_mortar") - 0.5)


def bastion_mortar_turntable():
    g = GUNS["bastion_mortar"]
    ty = g["trunnion"][1] - g["turntable"][1]
    tz = g["trunnion"][2]
    m = deck(18)
    m += railing(16.5, 3, 10, 120, 240)
    for x0, x1 in ((-13, -9), (9, 13)):
        m += cradle_side(x0, x1, ty, tz, 11, 5)
    m += pipe_run([(-10, 4, -12), (-10, 14, -12), (-10, 14, -6)])
    m += shell_rack(4, -14, 3)
    m.append(box((13, 3, -6), (16, 9, 0), {"*": YELLOW, "east": WARNING}))
    return m


def bastion_mortar_cradle():
    return fat_breech(8)


def bastion_mortar_barrel():
    return fat_barrel(6.5, GUNS["bastion_mortar"]["muzzle"], 8)


# ------------------------------------------------------------------ the Bastion Autocannon (3x3)

def bastion_autocannon_base():
    return plinth(footprint_radius("bastion_autocannon") - 0.5)


def bastion_autocannon_turntable():
    g = GUNS["bastion_autocannon"]
    ty = g["trunnion"][1] - g["turntable"][1]
    tz = g["trunnion"][2]
    m = deck(18)
    m += railing(16.5, 3, 10, 135, 225)
    # The gunhouse: a yellow armoured box open at the back. Its roof stops short of the front over a mantlet slot, so the
    # barrels' housing swings up through it at every elevation instead of cutting the roof.
    m.append(box((-13, 3, -6), (-8, ty + 4, tz + 4), YELLOW))
    m.append(box((8, 3, -6), (13, ty + 4, tz + 4), YELLOW))
    m.append(box((-13, ty + 4, -6), (13, ty + 6, -1), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((-13, ty + 4, -1), (-8.5, ty + 6, tz + 4), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((8.5, ty + 4, -1), (13, ty + 6, tz + 4), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((-13.5, 3, -6.5), (13.5, 4.5, tz + 4.5), BAND))
    # A port cover on each wall, above the ammunition drums.
    m.append(port_plate("west", -13, ty - 3.5, ty + 3.5, 1, 8))
    m.append(port_plate("east", 13, ty - 3.5, ty + 3.5, 1, 8))
    # Ammunition drums on the sides and a sight on the roof, behind the slot.
    for x in (-15, 15):
        m += cyl("z", x, 9, 3, -4, 4, OLIVE, BRASS)
    m.append(box((-1, ty + 6, -4), (1, ty + 9, -2), NUT))
    return m


def bastion_autocannon_cradle():
    return [box((-8, -5, -6), (8, 5, 4), {"*": GUNMETAL, "south": YELLOW})]


def bastion_autocannon_barrel():
    g = GUNS["bastion_autocannon"]
    m = []
    for x in g["barrels"]:
        m += cyl("z", x, 0, 1.5, 4, g["muzzle"] - 3, TUBE)
        m += cyl("z", x, 0, 2.4, 4, 12, TUBE)
        m += cyl("z", x, 0, 2.2, g["muzzle"] - 5, g["muzzle"], TUBE)
        m.append(bore(g["muzzle"], 1.1, 2.2, x))
    return m


# ------------------------------------------------------------------ the Grand Mortar (5x5)

def grand_mortar_base():
    return plinth(footprint_radius("grand_mortar") - 0.5, tall=True)


def grand_mortar_turntable():
    g = GUNS["grand_mortar"]
    ty = g["trunnion"][1] - g["turntable"][1]
    tz = g["trunnion"][2]
    m = deck(30)
    m += railing(28.5, 3, 12, 105, 255, step=25)
    for x0, x1 in ((-22, -15), (15, 22)):
        m += cradle_side(x0, x1, ty, tz, 20, 8)
    # The elevation gear beside the left cheek and its drive motor.
    m += cyl("x", ty - 4, tz - 2, 13, -25, -23, BAND, SKID)
    m.append(box((-27, 3, -18), (-22, 12, -8), {"*": GUNMETAL, "west": GRILLE}))
    # Pipework arching over the cheeks, as in the picture's looping pipes.
    m += pipe_run([(-18.5, 3, -24), (-18.5, 30, -24), (-18.5, 30, -14)], 1.6)
    m += pipe_run([(18.5, 3, -24), (18.5, 30, -24), (18.5, 30, -14)], 1.6)
    # A hazard sign, the control console with a lamp, the shell rack and a ladder down the back.
    m.append(box((22, 12, -6), (23.5, 22, 4), {"*": YELLOW, "east": WARNING}))
    m.append(box((8, 3, -27), (16, 12, -21), {"*": GUNMETAL, "north": GRILLE}))
    m.append(box((10, 12, -25), (14, 14, -23), AMBER))
    m += shell_rack(-14, -24, 4)
    for y in range(-8, 3, 3):
        m.append(box((-3, y, -31), (3, y + 1, -30), SKID))
    m.append(box((-3.5, -9, -31), (-2.5, 3, -30), SKID))
    m.append(box((2.5, -9, -31), (3.5, 3, -30), SKID))
    return m


def grand_mortar_cradle():
    m = fat_breech(14)
    # Twin recuperator cylinders riding on top of the breech.
    for x in (-5, 5):
        m += cyl("z", x, 16, 3, -18, 22, TUBE, NUT)
    return m


def grand_mortar_barrel():
    return fat_barrel(11, GUNS["grand_mortar"]["muzzle"], 14)


# ------------------------------------------------------------------ the Fortress Rifle (5x5)

def fortress_rifle_base():
    return plinth(footprint_radius("fortress_rifle") - 0.5, tall=True)


def fortress_rifle_turntable():
    g = GUNS["fortress_rifle"]
    ty = g["trunnion"][1] - g["turntable"][1]
    tz = g["trunnion"][2]
    m = deck(30)
    m += railing(28.5, 3, 12, 135, 225, step=15)
    # The armoured gunhouse: slab sides with port covers, a sloped-looking roof in two steps and vents at the back. The
    # lower roof leaves a mantlet slot over the barrel's housing, which rises into it as the gun elevates; the upper step
    # bridges the slot above the housing's highest reach.
    m.append(box((-24, 3, -24), (-14, ty + 8, tz), YELLOW))
    m.append(box((14, 3, -24), (24, ty + 8, tz), YELLOW))
    m.append(port_plate("west", -24, 10, 22, -1, 11))
    m.append(port_plate("east", 24, 10, 22, -1, 11))
    m.append(box((-24, ty + 8, -24), (24, ty + 11, 0), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((-24, ty + 8, 0), (-12.5, ty + 11, tz - 4), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((12.5, ty + 8, 0), (24, ty + 11, tz - 4), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((-20, ty + 11, -20), (20, ty + 13, tz - 10), {"*": YELLOW, "up": GUNMETAL}))
    m.append(box((-14, 3, -24), (14, ty + 8, -21), {"*": YELLOW, "north": GRILLE}))
    m.append(box((-24.5, 3, -24.5), (24.5, 4.5, tz + 0.5), BAND))
    # The range-finder arms across the roof, with a lens at each end.
    m += cyl("x", ty + 15, -14, 2, -28, 28, TUBE, AMBER)
    m.append(box((-2, ty + 13, -15.75), (2, ty + 15, -12.25), NUT))
    m.append(box((24, 10, -14), (25.5, 20, -4), {"*": YELLOW, "east": WARNING}))
    return m


def fortress_rifle_cradle():
    return [box((-12, -10, -12), (12, 10, 4), {"*": YELLOW, "south": GUNMETAL})]


def fortress_rifle_barrel():
    g = GUNS["fortress_rifle"]
    length = g["muzzle"]
    m = cyl("z", 0, 0, 4.5, 4, length - 7, TUBE)
    m += cyl("z", 0, 0, 6, 4, 30, TUBE)
    m += cyl("z", 0, 0, 5.5, 54, 58, TUBE)
    # The muzzle brake: a block with vent slots and the bore in its face.
    m.append(box((-6, -5, length - 8), (6, 5, length), GUNMETAL))
    for z in (length - 6, length - 3):
        m.append(box((-6.5, -2, z), (6.5, 2, z + 1.5), SOOT))
    m.append(bore(length, 3.4, 6))
    return m


# ------------------------------------------------------------------ the Triple Battery (5x5)

def triple_battery_base():
    return plinth(footprint_radius("triple_battery") - 0.5, tall=True)


def triple_battery_turntable():
    g = GUNS["triple_battery"]
    ty = g["trunnion"][1] - g["turntable"][1]
    m = deck(30)
    m += railing(28.5, 3, 12, 150, 210, step=15)
    # A low round turret drum with a domed roof, ports on its flanks and a commander's cupola.
    m += cyl("y", 0, -2, 25, 3, ty + 6, YELLOW, GUNMETAL)
    m += cyl("y", 0, -2, 25.5, 3, 4.5, BAND)
    m += cyl("y", 0, -4, 18, ty + 6, ty + 8, YELLOW, GUNMETAL)
    m += cyl("y", -10, -12, 4, ty + 8, ty + 12, GUNMETAL, AMBER)
    for x in (-25.6, 24.1):
        m.append(box((x, 6, -10), (x + 1.5, 18, 2), {"*": YELLOW, "east" if x > 0 else "west": PORT}))
    m.append(box((-6, 6, -28), (6, 16, -26.5), {"*": YELLOW, "north": WARNING}))
    return m


def triple_battery_cradle():
    # The housing's front stands half a pixel clear of the drum's front step, so they never share a plane at any
    # elevation.
    return [box((-17, -7, -8), (17, 7, 4.5), {"*": YELLOW, "south": GUNMETAL})]


def triple_battery_barrel():
    g = GUNS["triple_battery"]
    # The sleeves stand a quarter pixel clear of the drum's side facets.
    m = []
    for x in g["barrels"]:
        m += cyl("z", x, 0, 3, 4, g["muzzle"] - 3, TUBE)
        m += cyl("z", x, 0, 4.25, 4, 22, TUBE)
        m += cyl("z", x, 0, 3.8, g["muzzle"] - 5, g["muzzle"], TUBE)
        m.append(bore(g["muzzle"], 2.2, 3.8, x))
    return m


PARTS = {
    "bastion_mortar": (bastion_mortar_base, bastion_mortar_turntable, bastion_mortar_barrel),
    "bastion_autocannon": (bastion_autocannon_base, bastion_autocannon_turntable, bastion_autocannon_barrel),
    "grand_mortar": (grand_mortar_base, grand_mortar_turntable, grand_mortar_barrel),
    "fortress_rifle": (fortress_rifle_base, fortress_rifle_turntable, fortress_rifle_barrel),
    "triple_battery": (triple_battery_base, triple_battery_turntable, triple_battery_barrel),
}
# Each gun's cradle: the breech or housing that elevates with the barrel but stays put when it fires, while the barrel
# (PARTS) recoils back through it. client/TowerGunRenderer draws {gun}_cradle before the recoil and {gun}_barrel after.
CRADLES = {
    "bastion_mortar": bastion_mortar_cradle,
    "bastion_autocannon": bastion_autocannon_cradle,
    "grand_mortar": grand_mortar_cradle,
    "fortress_rifle": fortress_rifle_cradle,
    "triple_battery": triple_battery_cradle,
}


def export():
    out = {}
    for gun, (base, turntable, barrel) in PARTS.items():
        out[f"{gun}_base"] = tiled_quads(base())
        out[f"{gun}_turntable"] = tiled_quads(turntable())
        out[f"{gun}_cradle"] = tiled_quads(CRADLES[gun]())
        out[f"{gun}_barrel"] = tiled_quads(barrel())
    return out


# ------------------------------------------------------------------ the Java the numbers must match

def _f(value):
    text = repr(float(value))
    return text + "F"


def java_spec(gun):
    """The line in artillery/JugcraftTowerGuns.java that defines a gun (tools/check_mod_data.py looks for it)."""
    g = GUNS[gun]
    seats = ", ".join(f"new Vec3({x / 16}, {y / 16}, {z / 16})" for x, y, z in g["seats"])
    barrels = ", ".join(str(b / 16) for b in g["barrels"])
    return (f'spec("{gun}", {g["size"]}, "{g["shell"]}", {g["speed"]}, {g["cooldown"]}, {_f(g["traverse"])}, '
            f'{_f(g["pitch"][0])}, {_f(g["pitch"][1])}, {str(g["high"]).lower()}, {str(g["auto"]).lower()}, '
            f'{g["trunnion"][1] / 16}, {g["trunnion"][2] / 16}, {g["muzzle"] / 16}, {g["health"]}, {_f(g["height"])}, '
            f'new double[] {{{barrels}}}, {seats})')


def java_pivots(gun):
    """The line in client/TowerGunRenderer.java with a gun's pivots and recoil."""
    g = GUNS[gun]
    pivots = ", ".join("new float[] {" + ", ".join(str(v) for v in p) + "}" for p in (g["turntable"], g["trunnion"]))
    return f'pivots("{gun}", {pivots}, {_f(g["recoil"])})'


# ------------------------------------------------------------------ data

RECIPES = {
    "bastion_mortar": (["PGP", "PMP", "CCC"], {"P": "#c:plates/steel", "G": "#c:gears/steel",
                                              "M": f"{MOD}:siege_mortar", "C": "minecraft:smooth_stone"}, 1),
    "bastion_autocannon": (["PDP", "GFG", "CCC"], {"P": "#c:plates/steel", "D": "minecraft:dispenser",
                                                  "G": "#c:gears/steel", "F": f"{MOD}:flak_gun",
                                                  "C": "minecraft:smooth_stone"}, 1),
    "grand_mortar": (["SBS", "EGE", "CCC"], {"S": "#c:storage_blocks/steel", "B": f"{MOD}:bastion_mortar",
                                            "E": f"{MOD}:diesel_engine", "G": "#c:gears/steel",
                                            "C": "minecraft:smooth_stone"}, 1),
    "fortress_rifle": (["SDS", "EGE", "CCC"], {"S": "#c:storage_blocks/steel", "D": "minecraft:dispenser",
                                              "E": f"{MOD}:diesel_engine", "G": "#c:gears/steel",
                                              "C": "minecraft:smooth_stone"}, 1),
    "triple_battery": (["DDD", "EGE", "SCS"], {"D": "minecraft:dispenser", "E": f"{MOD}:diesel_engine",
                                              "G": "#c:gears/steel", "S": "#c:storage_blocks/steel",
                                              "C": "minecraft:smooth_stone"}, 1),
    "great_shell": (["HTH"], {"H": f"{MOD}:heavy_shell", "T": "minecraft:tnt"}, 1),
}


def write_all(write, assets, data, lang, condition):
    names = {gun: g["name"] for gun, g in GUNS.items()}
    names["great_shell"] = "Great Shell"
    for item, name in names.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
        lang[f"entity.{MOD}.{item}"] = name
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"message.{MOD}.artillery.needs_top"] = "Needs a solid %1$sx%1$s top to stand on"
    (assets / "tower_gun_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    for item, (pattern, key, count) in RECIPES.items():
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped",
            "category": "equipment" if item == "great_shell" else "misc",
            "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{item}", "count": count}})


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

PAINT = [(130, 96, 18), (176, 134, 28), (214, 170, 42), (236, 196, 72), (250, 220, 120)]
DARK = [(30, 30, 34), (44, 44, 48), (62, 62, 66), (86, 86, 90)]
STONE = [(100, 98, 92), (118, 116, 108), (134, 132, 124), (150, 148, 140)]


def port():
    """A round port cover on yellow paint: a dark steel ring, an X brace across it and a bolt at the centre."""
    img = clean_metal.canvas(PAINT[2])
    clean_metal.bevel(img, 0, 0, 15, 15, PAINT[3], PAINT[1], PAINT[2])
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.6:
                c = DARK[2] if d > 5.4 else DARK[1]
                if d > 5.4 and x + y < 15:
                    c = DARK[3]
                if d <= 5.4 and (abs(x - y) <= 0 or abs(x + y - 15) <= 0):
                    c = DARK[3]
                clean_metal.put(img, x, y, c)
    clean_metal.bolt(img, 7, 7, DARK)
    return img


def warning():
    """A hazard sign on yellow paint: a black-edged triangle with an exclamation mark."""
    img = clean_metal.canvas(PAINT[2])
    clean_metal.bevel(img, 0, 0, 15, 15, PAINT[3], PAINT[1], PAINT[2])
    for y in range(3, 13):
        half = (y - 3) * 0.6 + 0.5
        for x in range(16):
            if abs(x - 7.5) <= half:
                edge = abs(x - 7.5) > half - 1 or y == 12
                clean_metal.put(img, x, y, DARK[0] if edge else PAINT[4])
    for y in (6, 7, 8):
        clean_metal.put(img, 7, y, DARK[0])
        clean_metal.put(img, 8, y, DARK[0])
    clean_metal.put(img, 7, 10, DARK[0])
    clean_metal.put(img, 8, 10, DARK[0])
    return img


def slots():
    """The plinth's upper ring: cast concrete with a row of dark vent slots, each lit on its lower lip."""
    img = clean_metal.canvas(STONE[2])
    for x in range(16):
        clean_metal.put(img, x, 0, STONE[3])
        clean_metal.put(img, x, 15, STONE[1])
    for x0 in (1, 9):
        clean_metal.rect(img, x0, 5, x0 + 5, 9, DARK[0])
        for x in range(x0, x0 + 6):
            clean_metal.put(img, x, 10, STONE[3])
        for x in range(x0 + 1, x0 + 5, 2):
            for y in range(5, 10):
                clean_metal.put(img, x, y, DARK[1])
    return img


TUBE_PAL = [(54, 56, 60), (63, 65, 69), (72, 74, 78), (81, 83, 87), (96, 98, 102)]


def tube():
    """Barrel steel: one flat shade with a few faint marks in a fixed pattern, one shade either side, and no bevel, bolt
    or streak. A barrel shows it four ways round and repeats it every 16 pixels, so anything with a direction or a
    frame would read as stacked crates; the barrel's shape comes from its steps, bands and the light."""
    img = clean_metal.canvas(TUBE_PAL[2])
    for x, y in ((2, 3), (10, 1), (6, 11), (13, 9)):
        clean_metal.put(img, x, y, TUBE_PAL[3])
    for x, y in ((5, 6), (14, 14), (1, 13), (9, 7)):
        clean_metal.put(img, x, y, TUBE_PAL[1])
    return img


def steel():
    """Plate steel that tiles without a frame: long plates a tile high with no joints across them, so a roof or a housing
    reads as rolled plate rather than brick or paving (5 October 2026: the earlier offset blocks read as dark stone). Each
    plate is lit along its top edge and shaded into a dark seam along its foot, and is flat in between."""
    img = clean_metal.canvas(TUBE_PAL[2])
    for x in range(16):
        clean_metal.put(img, x, 0, TUBE_PAL[4])
        clean_metal.put(img, x, 1, TUBE_PAL[3])
        clean_metal.put(img, x, 14, TUBE_PAL[1])
        clean_metal.put(img, x, 15, TUBE_PAL[0])
    return img


SOOT_PAL = [(22, 21, 20), (30, 29, 28), (38, 36, 34)]


def soot():
    """A flat sooty dark for vents, slots and exhaust mouths, with a few marks one shade off in a fixed pattern."""
    img = clean_metal.canvas(SOOT_PAL[1])
    for x, y in ((3, 4), (11, 2), (7, 12), (14, 9)):
        clean_metal.put(img, x, y, SOOT_PAL[0])
    for x, y in ((9, 6), (2, 10)):
        clean_metal.put(img, x, y, SOOT_PAL[2])
    return img


def bore_decal():
    """A muzzle's bore, drawn whole on its plate: barrel steel round a dark round mouth whose radius is three quarters of
    the plate's half-width; inside it the lip is shaded at the top left and caught by the light at the bottom right, as
    a hole lit from the top left is, and a darker core shows the depth."""
    img = clean_metal.canvas(TUBE_PAL[2])
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d < 6.0:
                if d >= 4.8:
                    c = (34, 34, 38) if dx + dy < 0 else (78, 80, 84)
                elif d >= 3.6:
                    c = (24, 24, 27)
                else:
                    c = (14, 14, 16)
                clean_metal.put(img, x, y, c)
            elif d < 6.9:
                clean_metal.put(img, x, y, TUBE_PAL[1] if dx + dy < 0 else TUBE_PAL[3])
    return img


def draw_all(save):
    for name, img in (("tg_port", port()), ("tg_warning", warning()), ("tg_slots", slots()), ("tg_tube", tube()),
                      ("tg_steel", steel()), ("tg_soot", soot()), ("tg_bore", bore_decal())):
        save(img, "block", name)
    for item in list(GUNS) + ["great_shell"]:
        save(item_icons.draw(item), "item", item)
