"""Big guns (batch 51, docs/features/big-guns.md): heavy artillery for the dieselpunk front.

- The Siege Mortar: a fixed emplacement on a concrete ring, a railed turntable deck, a yellow cradle and a fat black
  barrel that lobs Heavy Shells in a high arc.
- The Self-Propelled Howitzer: a tracked gun carriage with an armoured cab and a long-barrelled gun with a muzzle brake
  that fires Heavy Shells flat or high.
- The Flak Gun: twin autocannon on a cross mount whose Flak Shells burst next to anything flying.
- The Observation Balloon: a tethered kite balloon whose basket rises high above its anchor while someone rides it, so
  a spotter with a Range Finder can mark targets far away for the guns.

Every shell bursts in a Blast (weapons/Blast): it hurts living things and never breaks a block. The guns aim
themselves at a target marked with a Range Finder (the gunner's own mark, else the nearest spotter's) and work out the
elevation that lands the shell on it; without a mark they fire where the gunner looks.

Java: artillery/ (the entities, shells, items and JugcraftArtillery); client/ArtilleryRenderers draws the parts
exported here (assets/jugcraft/artillery_quads.json). tools/check_mod_data.py keeps the numbers the same.
"""
import json
import math

from PIL import Image

import item_icons
from steampunk_models import box, cyl
from tower_guns import STEEL, TUBE, bore
from zeppelin import tiled_quads

MOD = "jugcraft"

# Shells: muzzle speed (blocks a tick), gravity (blocks a tick, a tick), blast reach and centre damage.
HEAVY_SPEED = 3.0
HEAVY_GRAVITY = 0.05
HEAVY_RADIUS = 5.0
HEAVY_DAMAGE = 32
FLAK_SPEED = 4.0
FLAK_GRAVITY = 0.01
FLAK_RADIUS = 3.0
FLAK_DAMAGE = 10
FLAK_FUSE = 30
# How close a flak shell must pass to something flying to burst, in blocks.
FLAK_PROXIMITY = 2.5
# Ticks between shots.
MORTAR_COOLDOWN = 100
HOWITZER_COOLDOWN = 80
FLAK_COOLDOWN = 8
# How fast a gun traverses and elevates, in degrees a tick.
MORTAR_TRAVERSE = 2.0
HOWITZER_TRAVERSE = 3.0
FLAK_TRAVERSE = 12.0
# The howitzer's gun turns this far either side of the hull.
HOWITZER_ARC = 30
# The howitzer drives like the landship, slower.
HOWITZER_SPEED = 0.1
HOWITZER_TURN = 2.0
HOWITZER_FUEL_TANK = 6000
FUEL_PER_BUCKET = 1000
HOWITZER_FUEL_PER_SECOND = 5
# The balloon: how high its basket rises above the anchor, and how fast it climbs and sinks.
BALLOON_HEIGHT = 32
BALLOON_CLIMB = 0.08
# The range finder's reach and how long a mark lasts (ticks).
MARK_RANGE = 256
MARK_TTL = 6000
HEALTH = {"siege_mortar": 150, "self_propelled_howitzer": 140, "flak_gun": 60, "observation_balloon": 30}

ITEMS = {
    "siege_mortar": "Siege Mortar",
    "self_propelled_howitzer": "Self-Propelled Howitzer",
    "flak_gun": "Flak Gun",
    "observation_balloon": "Observation Balloon",
    "heavy_shell": "Heavy Shell",
    "flak_shell": "Flak Shell",
    "range_finder": "Range Finder",
}
TOOLTIPS = {
    "siege_mortar": "A fixed heavy mortar. Climb aboard and press attack to lob a Heavy Shell at your marked target, or "
                    "where you look. Its bursts hurt creatures but never break blocks.",
    "self_propelled_howitzer": "A tracked gun. Drive with the movement keys; attack fires a Heavy Shell at your marked "
                               "target, or where you look. Refuel with a diesel or kerosene bucket.",
    "flak_gun": "Twin anti-aircraft cannon. Hold attack to fire Flak Shells that burst beside anything flying.",
    "observation_balloon": "A tethered balloon. Climb into its basket and it rises high above where you placed it; "
                           "climb out and it winches down.",
    "heavy_shell": "Ammunition for the Siege Mortar and the Self-Propelled Howitzer.",
    "flak_shell": "Ammunition for the Flak Gun.",
    "range_finder": "Use to mark the block you look at, up to 256 blocks away, as a target for your guns and any "
                    "gunner nearby. Sneak and use to clear your mark.",
}
ENTITIES = ["siege_mortar", "self_propelled_howitzer", "flak_gun", "observation_balloon", "heavy_shell", "flak_shell"]

# The guns share the tower guns' clean steel (tools/tower_guns.py): tg_tube for barrels, tg_steel for brakes and
# housings, tg_soot for vents and exhaust mouths, and a tg_bore decal in each muzzle.
YELLOW, OLIVE, HAZARD, CONCRETE, DECK = "ar_yellow", "ar_olive", "dp_hazard", "ar_concrete", "ar_deck"
GUNMETAL, SOOT = STEEL, "tg_soot"
SKID, BAND, NUT, BRASS, LACQUER, COPPER = "dr_skid", "dr_band", "dr_nut", "ik_brass", "ik_lacquer", "dr_copper_pipe"
EXHAUST, WICKER, ARMOR = "dp_exhaust", "ar_wicker", "ar_armor"

# Pivots, in pixels from the entity's feet (facing +z). Keep in sync with client/ArtilleryRenderers.
MORTAR_TURNTABLE = (0, 10, 0)
MORTAR_TRUNNION = (0, 30, 2)
HOWITZER_GUN = (6, 32, 4)
FLAK_HEAD = (0, 18, 0)
# Each gun's elevation limits in degrees (artillery/SiegeMortar, SelfPropelledHowitzer and FlakGun) and how far its
# barrel kicks back when it fires, in pixels (client/ArtilleryRenderers): tools/gun_poses.py poses the guns over these.
MORTAR_PITCH, HOWITZER_PITCH, FLAK_PITCH = (45, 85), (-5, 70), (-5, 85)
MORTAR_RECOIL, HOWITZER_RECOIL, FLAK_RECOIL = 6, 8, 2
# The howitzer's track path round each track unit, (z, y) in pixels, and the units' x span.
HOWITZER_TRACK = [(-38, 1), (36, 1), (44, 8), (44, 16), (36, 23), (-38, 23), (-46, 16), (-46, 8)]
HOWITZER_TRACK_X = (17, 29)


# ------------------------------------------------------------------ the siege mortar

def mortar_base():
    """The fixed concrete ring with its steel rim and four anchor feet."""
    m = cyl("y", 0, 0, 26, 0, 6, CONCRETE, CONCRETE)
    m += cyl("y", 0, 0, 27, 5, 7, BAND)
    # The bearing ring rises to the turntable's pivot, so no slit shows under the deck.
    m += cyl("y", 0, 0, 21, 6, MORTAR_TURNTABLE[1], SKID, DECK)
    for x, z in ((-26, -26), (22, -26), (-26, 22), (22, 22)):
        m.append(box((x, 0, z), (x + 4, 3, z + 4), SKID))
    return m


def mortar_turntable():
    """The turning deck, about its pivot: tread plate, hazard edge, railings on three sides, the cradle side plates
    and trunnion hubs, an elevation wheel and an ammunition rack."""
    # The deck's top is the hazard rim's tread cap, as on the tower guns, so the tread and the stripes never share a plane.
    m = cyl("y", 0, 0, 20, 0, 2, SKID, DECK)
    m += cyl("y", 0, 0, 20.5, 2, 3, HAZARD, DECK)
    # Railings round the back and sides; the top rails ride half a pixel above the cradle plates' tops.
    for angle in range(90, 271, 30):
        a = math.radians(angle)
        x, z = 18.5 * math.sin(a), 18.5 * math.cos(a)
        m.append(box((x - 0.5, 3, z - 0.5), (x + 0.5, 14.5, z + 0.5), SKID))
    for a0 in range(90, 270, 30):
        a, b = math.radians(a0), math.radians(a0 + 30)
        x0, z0, x1, z1 = 18.5 * math.sin(a), 18.5 * math.cos(a), 18.5 * math.sin(b), 18.5 * math.cos(b)
        mx, mz = (x0 + x1) / 2, (z0 + z1) / 2
        half = math.hypot(x1 - x0, z1 - z0) / 2
        angle = math.degrees(math.atan2(x1 - x0, z1 - z0))
        snapped = max(-45, min(45, round(((angle + 90) % 180 - 90) / 22.5) * 22.5))
        m.append(box((mx - 0.5, 13.5, mz - half), (mx + 0.5, 14.5, mz + half), SKID, rotation=("y", snapped, (mx, 13.5, mz))))
    # Cradle side plates rising to the trunnions, yellow with dark edging.
    tz = MORTAR_TRUNNION[2]
    for x0, x1 in ((-15, -10), (10, 15)):
        m.append(box((x0, 3, tz - 14), (x1, 14, tz + 10), {"*": YELLOW}))
        m.append(box((x0, 14, tz - 9), (x1, 26, tz + 7), {"*": YELLOW}))
        m.append(box((x0 - 0.5, 3, tz - 14.5), (x1 + 0.5, 4.5, tz + 10.5), BAND))
    ty = MORTAR_TRUNNION[1] - MORTAR_TURNTABLE[1]
    for x0, x1 in ((-17, -15), (15, 17)):
        m += cyl("x", ty, tz, 6, x0, x1, LACQUER, NUT)
    # The elevation wheel on the right and a rack of shells at the back.
    m += cyl("x", 10, tz + 4, 4, 17, 18, BRASS)
    for i in range(4):
        m += cyl("y", -6 + i * 4, -15, 1.6, 3, 10, BRASS, TUBE)
    return m


def mortar_cradle():
    """The cradle, about the trunnions, along +z: the yellow breech ring and the recuperator under the barrel. It
    elevates with the barrel but stays put when it fires, while the barrel recoils back through it."""
    # The breech stops a pixel short of the old -16, so its lower rear corner clears the deck at the steepest elevations.
    m = [box((-9, -9, -15), (9, 9, 4), {"*": YELLOW})]
    m.append(box((-9.5, -9.5, -6), (9.5, 9.5, -4), BAND))
    m += cyl("z", 0, -10.5, 2.5, -10, 22, COPPER)
    return m


def mortar_barrel():
    """The barrel, along +z out of its breech: a fat black tube with a sleeve, which stays a quarter pixel inside the
    breech's sides as it recoils into it, and a belled muzzle round the bore."""
    m = cyl("z", 0, 0, 8, 4, 53, TUBE)
    m += cyl("z", 0, 0, 8.75, 4, 14, TUBE)
    m += cyl("z", 0, 0, 9.5, 50, 56, TUBE)
    m.append(bore(56, 6, 9.5))
    return m


# ------------------------------------------------------------------ the self-propelled howitzer

def howitzer_body():
    """The carriage: two track units, the hull between them, an armoured cab at the back left, a low engine deck at the
    front right with its exhausts behind the crew, and the gun's mounting ring. The gun sweeps 30 degrees either side
    and from -5 to 70 degrees up without passing through any of it (tools/check_mod_data.py samples the sweep)."""
    m = []
    x0, x1 = HOWITZER_TRACK_X
    for sign in (1, -1):
        a, b = (x0, x1) if sign > 0 else (-x1, -x0)
        m.append(box((a + 1, 4, -38), (b - 1, 20, 38), {"*": ARMOR, "up": SKID}))
        for z in (-28, -12, 4, 20):
            # Wheel hubs on the units' outer faces, standing a quarter pixel proud of the track links' sides.
            m += cyl("x", 6, z, 4.5, *((a + 11, a + 12.25) if sign > 0 else (a - 0.25, a + 1)), SKID, NUT)
    m.append(box((-17, 6, -36), (17, 22, 30), {"*": ARMOR, "up": DECK}))
    m.append(box((-17.5, 21, -36.5), (17.5, 23, 30.5), {"*": BAND, "up": DECK}))
    # The cab: tall, slab-sided, vision slits, a hatch and a rail.
    m.append(box((-17, 22, -36), (-1, 46, -12), {"*": ARMOR}))
    m.append(box((-15, 46, -32), (-3, 48, -16), {"*": OLIVE}))
    for z in (-34, -26, -18):
        m.append(box((-17.5, 38, z), (-16.5, 39.5, z + 4), NUT))
    m.append(box((-14, 38, -12), (-4, 39.5, -11.5), NUT))
    # The engine under a low grille deck at the front right (clear of the gun's sweep), and two exhausts at the back
    # right, behind the crew.
    m.append(box((2, 23, 16), (16, 25, 30), {"*": ARMOR, "up": "dp_grille"}))
    for x in (4, 14):
        m += cyl("y", x, -34, 1.6, 23, 36, EXHAUST, SOOT)
    # The gun's mounting ring.
    gx, gy, gz = HOWITZER_GUN
    m += cyl("y", gx, gz, 8, 22, 24, SKID, DECK)
    return m


def howitzer_gun():
    """The gun's cradle, about its pivot, along +z: the cradle with its recoil cylinders and the armoured shield. It
    turns and elevates with the barrel but stays put when it fires, while the barrel recoils back through it."""
    m = [box((-6, -5, -7), (6, 6, 12), {"*": ARMOR})]
    m.append(box((-11, -6.5, 8), (11, 10, 10), {"*": ARMOR}))
    for x in (-4, 4):
        m += cyl("z", x, 7, 2, -10, 20, COPPER)
    return m


def howitzer_barrel():
    """The howitzer's barrel, along +z out of its cradle: a long tube with a sleeve and a muzzle brake round the bore."""
    m = cyl("z", 0, 0, 3.5, 10, 89, TUBE)
    m += cyl("z", 0, 0, 4.5, 10, 24, TUBE)
    m.append(box((-5, -4, 88), (5, 4, 96), {"*": GUNMETAL}))
    for z in (90, 93):
        m.append(box((-5.5, -2, z), (5.5, 2, z + 1.5), SOOT))
    m.append(bore(96, 2.6, 5))
    return m


# ------------------------------------------------------------------ the flak gun

def flak_mount():
    """The fixed cross mount: four splayed legs with feet and a pedestal."""
    m = []
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        x0, x1 = (min(0, dx * 22), max(0, dx * 22)) if dx else (-2, 2)
        z0, z1 = (min(0, dz * 22), max(0, dz * 22)) if dz else (-2, 2)
        m.append(box((x0, 0, z0), (x1, 3, z1), {"*": OLIVE}))
        fx, fz = dx * 21, dz * 21
        m.append(box((fx - 3, 0, fz - 3), (fx + 3, 1, fz + 3), SKID))
    m += cyl("y", 0, 0, 5, 3, 12, OLIVE, SKID)
    return m


def flak_head():
    """The turning head, about its pivot: the cradle, ammunition drums and the gunner's sights. It stays put when the
    gun fires, while the twin barrels (flak_barrels) recoil back into the cradle."""
    # The cradle's back stops at z -5, so at full elevation it swings clear of the pedestal.
    m = [box((-7, -4, -5), (7, 6, 8), {"*": OLIVE})]
    for x in (-4, 4):
        m += cyl("x", 7, -3.5, 4, x - 1.5, x + 1.5, OLIVE, BRASS)
    m.append(box((-1, 6, 2), (1, 10, 4), SKID))
    m.append(box((-2, 9, 1.5), (2, 12, 2.75), NUT))
    return m


def flak_barrels():
    """The twin barrels, along +z out of the cradle, with flash hiders round their bores."""
    m = []
    for x in (-4, 4):
        m += cyl("z", x, 1, 1.4, 8, 44, TUBE)
        m += cyl("z", x, 1, 2.2, 40, 46, TUBE)
        m.append(bore(46, 1.1, 2.2, x, 1))
    return m


# ------------------------------------------------------------------ the observation balloon

BALLOON_Y = 104
# The envelope, a smooth closed surface turned about the z axis at BALLOON_Y (5 October 2026: the owner found the old
# stair-stepped boxes' envelope had "tons of transparency"). Its widest ring is BALLOON_R pixels at BALLOON_WAIST; the nose reaches
# BALLOON_NOSE and the tail BALLOON_TAIL, each end rounded as a superellipse with the exponent beside it (the nose blunt,
# the tail a plain ellipse). Three inflated lobes (BALLOON_LOBES: their angle round the axis, in degrees) steady its tail.
BALLOON_R = 24
BALLOON_WAIST = -6
BALLOON_NOSE, NOSE_ROUND = 55, 2.3
BALLOON_TAIL, TAIL_ROUND = -62, 2.0
BALLOON_GORES = 24
BALLOON_LOBES = (90, 210, 330)
LOBE_OFFSET, LOBE_R, LOBE_Z = 15, 6.5, (-72, -46)
ENVELOPE_TEXTURE = "entity/observation_balloon/envelope"
# The envelope texture: BALLOON_GORES gores of GORE_TEXELS columns round it, the body in the top three quarters (nose at
# the top) and the lobes in the bottom quarter.
GORE_TEXELS = 8
ENVELOPE_SIZE = (BALLOON_GORES * GORE_TEXELS, 128)
BODY_V = 0.75


def _profile(phi, p):
    """A superellipse quarter: phi 0 at the waist, pi/2 at the pole; returns (distance along the axis, radius) as
    fractions of the half's length and of the waist radius."""
    c, s = math.cos(phi), math.sin(phi)
    return (abs(s) ** (2 / p), abs(c) ** (2 / p))


def _meridian(rings_tail=9, rings_nose=10):
    """The envelope's outline from tail pole to nose pole: [(z, r, nz, nr)] with the outward normal in the (z, r) plane.
    The rings are spaced evenly in the superellipse's angle, so they crowd where it curves most, at the poles."""
    out = []
    # The tail half from its pole to the waist, then the nose half on from the waist to its pole.
    halves = ((rings_tail, BALLOON_WAIST - BALLOON_TAIL, TAIL_ROUND, -1), (rings_nose, BALLOON_NOSE - BALLOON_WAIST, NOSE_ROUND, 1))
    for rings, length, p, sign in halves:
        steps = range(rings, -1, -1) if sign < 0 else range(1, rings + 1)
        for i in steps:
            phi = math.pi / 2 * i / rings
            t, r = _profile(phi, p)
            z = BALLOON_WAIST + sign * t * length
            # The outline's slope from two close points, for a normal that lights it smoothly.
            e = 1e-4
            t2, r2 = _profile(min(math.pi / 2, phi + e), p)
            t1, r1 = _profile(max(0.0, phi - e), p)
            dz, dr = sign * (t2 - t1) * length, (r2 - r1) * BALLOON_R
            nz, nr = sign * abs(dr), abs(dz)
            if i == rings:
                nz, nr = float(sign), 0.0
            n = math.hypot(nz, nr) or 1.0
            out.append((z, r * BALLOON_R, nz / n, nr / n))
    return out


def _lathe(outline, cx, cy, gores, u0, u1, v0, v1, texture):
    """Quads of a surface turned about the line x = cx, y = cy (along z): `outline` [(z, r, nz, nr)] from one pole to the
    other, `gores` segments round it, shared corners (no cracks), each corner with its own normal, wound to face out.
    The texture runs u0 to u1 round it and v0 to v1 from the first ring to the last, by length along the outline."""
    lengths = [0.0]
    for (za, ra, *_), (zb, rb, *_) in zip(outline, outline[1:]):
        lengths.append(lengths[-1] + math.hypot(zb - za, rb - ra))
    total = lengths[-1]
    out = []
    for j in range(len(outline) - 1):
        for k in range(gores):
            corners, uvs, normals = [], [], []
            for jj, kk in ((j, k), (j, k + 1), (j + 1, k + 1), (j + 1, k)):
                z, r, nz, nr = outline[jj]
                a = 2 * math.pi * kk / gores
                corners.append([cx + r * math.cos(a), cy + r * math.sin(a), z])
                normals.append([nr * math.cos(a), nr * math.sin(a), nz])
                uvs.append((u0 + (u1 - u0) * kk / gores, v0 + (v1 - v0) * lengths[jj] / total))
            # (c2 - c0) x (c3 - c1) must point the way the surface faces (outward).
            d1 = [corners[2][i] - corners[0][i] for i in range(3)]
            d2 = [corners[3][i] - corners[1][i] for i in range(3)]
            n = [d1[1] * d2[2] - d1[2] * d2[1], d1[2] * d2[0] - d1[0] * d2[2], d1[0] * d2[1] - d1[1] * d2[0]]
            size = math.sqrt(sum(c * c for c in n))
            if size < 1e-9:
                continue
            mid = [sum(c[i] for c in normals) for i in range(3)]
            if sum(n[i] * mid[i] for i in range(3)) < 0:
                corners, uvs, normals = corners[::-1], uvs[::-1], normals[::-1]
                n = [-c for c in n]
            out.append({"texture": texture, "normal": [round(c / size, 4) for c in n],
                        "normals": [[round(c, 3) for c in nn] for nn in normals],
                        "vertices": [[round(p[0], 3), round(p[1], 3), round(p[2], 3), round(u, 5), round(v, 5)]
                                     for p, (u, v) in zip(corners, uvs)]})
    return out


def lobe_outline(rings=8):
    """A tail lobe's outline: an ellipsoid LOBE_R across and as long as LOBE_Z."""
    z0, z1 = LOBE_Z
    half, mid = (z1 - z0) / 2, (z0 + z1) / 2
    out = []
    for i in range(rings + 1):
        phi = math.pi * i / rings - math.pi / 2
        z, r = mid + half * math.sin(phi), LOBE_R * math.cos(phi)
        nz, nr = math.sin(phi) / half, math.cos(phi) / LOBE_R
        n = math.hypot(nz, nr)
        out.append((z, max(0.0, r), nz / n, nr / n))
    return out


def envelope_quads():
    """The kite balloon high above its basket: the envelope turned smooth and closed (a quad mesh with no gaps, every
    quad facing out, so it draws solid with back faces culled), and its three tail lobes, all mapped once on
    ENVELOPE_TEXTURE (the painted bands and the serial are in the texture, not boxes)."""
    quads = _lathe(_meridian(), 0, BALLOON_Y, BALLOON_GORES, 0.0, 1.0, BODY_V, 0.0, ENVELOPE_TEXTURE)
    for angle in BALLOON_LOBES:
        a = math.radians(angle)
        quads += _lathe(lobe_outline(), LOBE_OFFSET * math.cos(a), BALLOON_Y + LOBE_OFFSET * math.sin(a), 12,
                        0.0, 1.0, 1.0, BODY_V + 0.02, ENVELOPE_TEXTURE)
    return quads


def envelope_row(z):
    """The texture row (from the top) where the envelope's ring at z is drawn."""
    outline = _meridian()
    lengths = [0.0]
    for (za, ra, *_), (zb, rb, *_) in zip(outline, outline[1:]):
        lengths.append(lengths[-1] + math.hypot(zb - za, rb - ra))
    for i, ((za, *_), (zb, *_)) in enumerate(zip(outline, outline[1:])):
        if za <= z <= zb:
            t = (z - za) / (zb - za) if zb != za else 0.0
            along = lengths[i] + (lengths[i + 1] - lengths[i]) * t
            return (BODY_V - BODY_V * along / lengths[-1]) * ENVELOPE_SIZE[1]
    raise ValueError(z)


def envelope_column(angle):
    """The texture column round the envelope at `angle` degrees about its axis (0 = +x, 90 = up)."""
    return (angle % 360) / 360 * ENVELOPE_SIZE[0]


def balloon_basket():
    """The wicker basket with its rim, the rigging lines up to the envelope and the winch cable's shackle below."""
    m = [box((-10, 0, -10), (10, 14, 10), {"*": WICKER})]
    m.append(box((-10.5, 13, -10.5), (10.5, 15, 10.5), {"*": SKID}))
    for x, z in ((-9, -9), (9, -9), (-9, 9), (9, 9)):
        m.append(box((x - 0.5, 15, z - 0.5), (x + 0.5, BALLOON_Y - 20, z + 0.5), BAND))
    m += cyl("y", 0, 0, 1.5, -2, 0, NUT)
    return m


def balloon_cable():
    """One block of the winch cable, hanging down from the origin; the renderer stretches it to the anchor."""
    return [box((-0.5, -16, -0.5), (0.5, 0, 0.5), BAND)]


def export():
    # Part names share one table with every other file client/DecorQuads loads (tools/check_mod_data.py checks that
    # none repeats): the basket is "observation_basket", since balloon_quads.json's "balloon_basket" is the hot-air one.
    # Each gun's cradle stays put when it fires and its barrel recoils back through it: client/ArtilleryRenderers
    # draws mortar_cradle, howitzer_gun and flak_head before the recoil, and mortar_barrel, howitzer_barrel and
    # flak_barrels after it.
    return {"mortar_base": tiled_quads(mortar_base()), "mortar_turntable": tiled_quads(mortar_turntable()),
            "mortar_cradle": tiled_quads(mortar_cradle()), "mortar_barrel": tiled_quads(mortar_barrel()),
            "howitzer_body": tiled_quads(howitzer_body()), "howitzer_gun": tiled_quads(howitzer_gun()),
            "howitzer_barrel": tiled_quads(howitzer_barrel()), "flak_mount": tiled_quads(flak_mount()),
            "flak_head": tiled_quads(flak_head()), "flak_barrels": tiled_quads(flak_barrels()),
            "balloon_envelope": envelope_quads(),
            "observation_basket": tiled_quads(balloon_basket()), "balloon_cable": tiled_quads(balloon_cable())}


RECIPES = {
    "siege_mortar": (["BGB", "PEP", "CCC"], {"B": "#c:plates/steel", "G": "#c:gears/steel", "P": "minecraft:piston",
                                            "E": "minecraft:dispenser", "C": "minecraft:smooth_stone"}, 1),
    "self_propelled_howitzer": (["PBP", "EDE", "TGT"], {"P": "#c:plates/steel", "B": "minecraft:dispenser",
                                                       "E": f"{MOD}:diesel_engine", "D": "minecraft:piston",
                                                       "T": f"{MOD}:belt", "G": "#c:gears/steel"}, 1),
    "flak_gun": (["D D", "PGP", "I I"], {"D": "minecraft:dispenser", "P": "#c:plates/steel", "G": "#c:gears/steel",
                                         "I": "minecraft:iron_ingot"}, 1),
    "observation_balloon": (["LLL", "LCL", " W "], {"L": f"{MOD}:hydrogen_lift_cell", "C": "minecraft:lead",
                                                    "W": "minecraft:barrel"}, 1),
    "heavy_shell": (["P", "G", "B"], {"P": "#c:plates/steel", "G": "minecraft:gunpowder", "B": "#c:plates/brass"}, 2),
    "flak_shell": (["PGP"], {"P": "#c:nuggets/brass", "G": "minecraft:gunpowder"}, 4),
    "range_finder": (["G G", "BCB"], {"G": "minecraft:glass_pane", "B": "#c:plates/brass", "C": "minecraft:compass"}, 1),
}


def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    for entity in ENTITIES:
        lang[f"entity.{MOD}.{entity}"] = ITEMS[entity]
    lang[f"message.{MOD}.artillery.no_room"] = "Not enough room here"
    lang[f"message.{MOD}.artillery.no_shells"] = "No %s"
    lang[f"message.{MOD}.artillery.marked"] = "Target marked %s blocks away"
    lang[f"message.{MOD}.artillery.cleared"] = "Target mark cleared"
    lang[f"message.{MOD}.artillery.no_target"] = "Nothing to mark within %s blocks"
    lang[f"message.{MOD}.artillery.out_of_range"] = "The marked target is out of range"
    lang[f"message.{MOD}.artillery.fuel"] = "Fuel: %s%%"
    lang[f"message.{MOD}.artillery.refuelled"] = "Refuelled: %s%%"
    lang[f"message.{MOD}.artillery.full"] = "The fuel tank is full"
    (assets / "artillery_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    for item, (pattern, key, count) in RECIPES.items():
        category = "equipment" if item in ("heavy_shell", "flak_shell", "range_finder") else "misc"
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": category,
            "pattern": pattern, "key": key, "result": {"id": f"{MOD}:{item}", "count": count}})


# ------------------------------------------------------------------ art

def _img():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def _put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), tuple(c) + (255,))




def painted(pal):
    """Painted armour that tiles without a frame (5 October 2026: big housings tiled with a framed, bolted panel read as
    stacks of crates): a flat coat, one welded seam across the foot of each 16-pixel course with its lit lip on the row
    above the next course, and two short scuffs one shade off; no bolts, bevelled frame or chips. pal runs dark to light:
    seam, scuff, coat, lip."""
    img = _img()
    _rect(img, 0, 0, 15, 15, pal[2])
    for x in range(16):
        _put(img, x, 15, pal[0])
        _put(img, x, 0, pal[3])
    for x, y, length, c in ((3, 5, 3, pal[3]), (10, 10, 2, pal[1])):
        for i in range(length):
            _put(img, x + i, y, c)
    return img


def yellow():
    """Warning-yellow paint on the gun housings."""
    return painted([(176, 134, 28), (200, 158, 36), (214, 170, 42), (234, 192, 70)])


def armor():
    """Khaki-bronze armour on the howitzer's hull and tracks."""
    return painted([(96, 86, 58), (114, 103, 71), (122, 110, 76), (140, 127, 89)])


def olive():
    """Olive drab on the flak gun and the howitzer's hatch."""
    return painted([(66, 74, 44), (80, 88, 54), (88, 96, 60), (104, 112, 72)])


ENVELOPE_PAL = [(150, 138, 104), (172, 160, 124), (188, 176, 138), (202, 192, 154)]
ENVELOPE_RED, ENVELOPE_CREAM, ENVELOPE_STENCIL = (150, 36, 28), (224, 214, 184), (62, 58, 50)
# Painted bands round the envelope (the z of each band's tail edge, in pixels) and their width.
ENVELOPE_BANDS, BAND_WIDTH = (-32, 14), 8
SERIAL = "51"
GLYPHS = {"5": ["111", "100", "111", "001", "111"], "1": ["010", "110", "010", "010", "111"]}


def envelope_texture():
    """The observation balloon's envelope, in the clean style: flat doped canvas, each gore's seam one shade down with a
    lit edge beside it, a sewn ring every 16 pixels along it, a darker reinforced nose, two red and cream bands, patches
    where the rigging meets it, and the stencilled serial on both flanks, reading level along the hull."""
    w, h = ENVELOPE_SIZE
    img = Image.new("RGBA", (w, h), ENVELOPE_PAL[2] + (255,))
    px = img.load()
    body = round(h * BODY_V)
    nose = round(envelope_row(BALLOON_NOSE - 7))

    def paint(x, y, c):
        px[x % w, y] = tuple(c) + (255,)

    for y in range(body):
        for x in range(w):
            g = x % GORE_TEXELS
            c = ENVELOPE_PAL[1] if g == 0 else ENVELOPE_PAL[3] if g == 1 else ENVELOPE_PAL[2]
            if y < nose:
                c = tuple(int(v * 0.86) for v in c)
            paint(x, y, c)
    for z in range(BALLOON_WAIST - 48, BALLOON_NOSE - 7, 16):
        y = round(envelope_row(z))
        for x in range(w):
            if x % GORE_TEXELS:
                paint(x, y, ENVELOPE_PAL[1])
    for z in ENVELOPE_BANDS:
        top, bottom = round(envelope_row(z + BAND_WIDTH)), round(envelope_row(z))
        for y in range(top, bottom):
            part = (y - top) / max(1, bottom - top)
            c = ENVELOPE_CREAM if 0.25 <= part < 0.75 else ENVELOPE_RED
            for x in range(w):
                paint(x, y, c)
    # Rigging patches, where the four lines from the basket meet the underside.
    for x0 in (-9, 9):
        column = round(envelope_column(math.degrees(math.atan2(-20, x0))))
        for z0 in (-9, 9):
            row = round(envelope_row(z0))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    paint(column + dx, row + dy, ENVELOPE_STENCIL if (dx, dy) == (0, 0) else ENVELOPE_PAL[0])
    # The serial on each flank, two texels to a glyph pixel, its top towards the envelope's top: on the +x flank the text
    # runs tail-wards and on the -x flank nose-wards, so from outside it reads left to right either way.
    bits = [[c == "1" for ch in SERIAL for c in GLYPHS[ch][row] + "0"][:-1] for row in range(5)]
    tall, long = 10, len(bits[0]) * 2
    middle = round(envelope_row(BALLOON_WAIST))
    for side, column in ((1, round(envelope_column(0))), (-1, round(envelope_column(180)))):
        for gy in range(tall):
            for gx in range(long):
                if bits[gy // 2][gx // 2]:
                    paint(column + side * (tall // 2 - gy), middle + side * (gx - long // 2), ENVELOPE_STENCIL)
    # The lobes, in the bottom quarter: twelve gores, seams as on the body.
    for y in range(body, h):
        for x in range(w):
            g = x % (w // 12)
            paint(x, y, ENVELOPE_PAL[1] if g == 0 else ENVELOPE_PAL[3] if g == 1 else ENVELOPE_PAL[2])
    return img


def concrete():
    """Cast concrete like vanilla smooth stone: a flat grey, a formwork seam along the bottom and a few soft blotches."""
    pal = [(100, 98, 92), (118, 116, 108), (134, 132, 124), (150, 148, 140)]
    img = _img()
    _rect(img, 0, 0, 15, 15, pal[2])
    for x in range(16):
        _put(img, x, 15, pal[1])
        _put(img, x, 0, pal[3])
    for pixels, c in (([(3, 4), (4, 4), (4, 5)], pal[1]), ([(10, 7), (11, 7)], pal[3]),
                      ([(6, 11), (7, 11), (7, 12)], pal[1]), ([(12, 12), (13, 12)], pal[3])):
        for x, y in pixels:
            _put(img, x, y, c)
    return img


def deck():
    """Diamond tread plate: flat dark steel with raised lozenges, each lit on its upper left."""
    img = _img()
    _rect(img, 0, 0, 15, 15, (72, 72, 74))
    for cy in (2, 10):
        for cx in (2, 10):
            for ox, oy in ((0, 0), (4, 4)):
                x, y = (cx + ox) % 16, (cy + oy) % 16
                _put(img, x, y, (142, 142, 144))
                _put(img, (x + 1) % 16, (y + 1) % 16, (112, 112, 114))
                _put(img, (x + 2) % 16, (y + 2) % 16, (52, 52, 54))
    return img


def wicker():
    """Woven wicker: alternating light and dark strands."""
    img = _img()
    for y in range(16):
        for x in range(16):
            over = ((x // 2) + (y // 2)) % 2 == 0
            c = (176, 140, 84) if over else (128, 96, 54)
            if (x % 2 == 0 and over) or (y % 2 == 0 and not over):
                c = tuple(v - 18 for v in c)
            _put(img, x, y, c)
    return img


def _rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            _put(img, x, y, c)


def draw_all(save):
    for name, img in (("ar_yellow", yellow()), ("ar_concrete", concrete()), ("ar_deck", deck()), ("ar_armor", armor()),
                      ("ar_wicker", wicker()), ("ar_olive", olive())):
        save(img, "block", name)
    folder, name = ENVELOPE_TEXTURE.rsplit("/", 1)
    save(envelope_texture(), folder, name)
    for item in ITEMS:
        save(item_icons.draw(item), "item", item)
