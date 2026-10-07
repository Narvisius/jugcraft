"""The Landship (batch 49, docs/features/landship.md): a rideable kaiserpunk crawler tank.

A rhomboid hull in the look of the first tanks, its tracks running right round each side frame, under a black
lacquered casemate with gilt trim, a big brass-rimmed hub bearing the imperial crest on each side, a sponson machine
gun on each flank, twin smokestacks at the back and a turret on top whose cannon follows where the driver looks.

The driver steers with the movement keys (it climbs one-block steps and its long hull bridges narrow trenches), fires
the cannon with attack (a cannon shell from their inventory each shot) and holds use for the sponson guns. The shell
bursts in a Blast (weapons/Blast): it hurts living things and never breaks a block. It burns diesel or kerosene.

Java: landship/ (Landship, LandshipShell, LandshipItem, LandshipInputPayload, JugcraftLandships);
client/LandshipRenderer animates the parts exported here (assets/jugcraft/landship_quads.json), laying the track links
along TRACK_PATH. tools/check_mod_data.py keeps the numbers the same.
"""
import json
import math

from PIL import Image

import item_icons
from steampunk_models import box, cyl
from tower_guns import TUBE, bore
from zeppelin import tiled_quads

MOD = "jugcraft"

# Movement, in blocks a tick and degrees a tick.
SPEED = 0.15
TURN = 2.5
# The cannon: ticks between shots, shell speed (blocks a tick), the blast's reach and centre damage.
CANNON_COOLDOWN = 40
SHELL_SPEED = 2.5
SHELL_RADIUS = 3.5
SHELL_DAMAGE = 22
# The sponson guns: ticks between bursts (the same as a creature's hurt cooldown), damage a hit and reach.
GUN_INTERVAL = 10
GUN_DAMAGE = 4
GUN_RANGE = 24
# Crushing: damage to anything the hull drives into.
CRUSH_DAMAGE = 4
# Fuel in mB: the tank, what a bucket adds and what driving burns a second.
FUEL_TANK = 6000
FUEL_PER_BUCKET = 1000
FUEL_PER_SECOND = 5
HEALTH = 120
SEATS = 3
WIDTH = 3.5
HEIGHT = 2.75

ITEMS = {"landship": "Landship", "cannon_shell": "Cannon Shell"}
TOOLTIPS = {
    "landship": "A crawler tank for three. Drive with the movement keys, fire the cannon with attack (it uses cannon "
                "shells) and hold use for the side guns. Refuel with a diesel or kerosene bucket.",
    "cannon_shell": "Ammunition for the Landship's cannon. Its burst hurts creatures but never breaks blocks.",
}

RUST, RUST_BARE, BAND, SKID, NUT = "dr_rust", "dr_rust_bare", "dr_band", "dr_skid", "dr_nut"
PERFORATED, DOME, EXHAUST, SOOT, AMBER = "dr_perforated", "dr_dome", "dp_exhaust", "dr_soot", "dr_amber_on"
LACQUER, RIVETED, GILT, BRASS, CREST, FRIEZE = "ik_lacquer", "ik_lacquer_riveted", "ik_gilt_trim", "ik_brass", "ik_crest", "ik_frieze"
TREAD = "ls_tread"

# The track's path round each side frame, in pixels as (z, y), in order: the links are laid along it. Keep in sync
# with client/LandshipRenderer.
TRACK_PATH = [(-30, 1), (28, 1), (42, 16), (32, 32), (-36, 30), (-42, 14)]
# The side frames span x = TRACK_INNER .. TRACK_OUTER on each side.
TRACK_INNER, TRACK_OUTER = 17, 28
# The pitch between track links, in pixels.
LINK_PITCH = 6
# The turret turns about TURRET; the barrel pitches about BARREL (from the turret).
TURRET = (0, 36, -2)
BARREL = (0, 6, 9)
# The barrel's pitch limits, in degrees of the driver's view (up is negative; landship/Landship BARREL_UP and BARREL_DOWN),
# and how far it kicks back when it fires, in pixels (client/LandshipRenderer RECOIL). tools/gun_poses.py poses it over these.
BARREL_PITCH = (-25, 10)
RECOIL = 4
# The smokestacks' tops, in pixels (landship/Landship puffs its smoke just above them).
STACK_TOP = 35.75


def _polygon_span(y, inset):
    """The z range of TRACK_PATH's polygon at height y, shrunk by inset."""
    zs = []
    pts = TRACK_PATH + TRACK_PATH[:1]
    for (z0, y0), (z1, y1) in zip(pts, pts[1:]):
        if (y0 - y) * (y1 - y) <= 0 and y0 != y1:
            zs.append(z0 + (y - y0) * (z1 - z0) / (y1 - y0))
    return min(zs) + inset, max(zs) - inset


def side_frame(sign):
    """One side's track frame: riveted steel plate filling the rhomboid inside the track, the hub with the crest, the
    sponson and its gun."""
    x0, x1 = (TRACK_INNER, TRACK_OUTER) if sign > 0 else (-TRACK_OUTER, -TRACK_INNER)
    m = []
    for y in range(4, 30, 2):
        z0, z1 = _polygon_span(y + 1, 4.5)
        m.append(box((x0 + 0.5, y, z0), (x1 - 0.5, y + 2, z1), {"*": RUST, "up": RUST_BARE, "down": RUST_BARE}))
    # A gilt rail along the top of the frame.
    m.append(box((x0, 27, -30), (x1, 28.5, 26), {"*": GILT}))
    # The hub, bearing the crest, towards the back.
    outer = x1 if sign > 0 else x0

    def out(d0, d1):
        """x from d0 to d1 pixels out from the frame's outer face."""
        return (outer + d0, outer + d1) if sign > 0 else (outer - d1, outer - d0)
    m += cyl("x", 15, -14, 9.5, *out(-0.5, 1.0), BRASS)
    m += cyl("x", 15, -14, 7.5, *out(1.0, 2.0), LACQUER)
    crest_face = "east" if sign > 0 else "west"
    m.append(box((out(2.0, 2.5)[0], 11, -18), (out(2.0, 2.5)[1], 19, -10), {"*": GILT, crest_face: f"{CREST}!"}))
    # The sponson: a half drum on the flank with a machine gun poking forward.
    s0, s1 = (outer, outer + 6) if sign > 0 else (outer - 6, outer)
    m.append(box((s0, 9, 4), (s1, 21, 16), {"*": RIVETED}))
    m.append(box((s0 - 0.25 * sign, 20, 3.5), (s1 + 0.25 * sign, 21.5, 16.5), {"*": GILT}))
    gx = outer + 3 * sign
    m += cyl("z", gx, 15, 1.2, 16, 25, TUBE)
    m += cyl("z", gx, 15, 1.8, 16, 18.5, BRASS)
    m.append(bore(25, 0.5, 1.2, gx, 15))
    return m


def body():
    """The hull between the side frames: the lower hull, the casemate, the engine deck, stacks and headlamps."""
    m = []
    m.append(box((-17, 5, -32), (17, 24, 32), {"*": RIVETED, "up": LACQUER}))
    # The casemate on top, with a gilt band and a frieze across its front.
    m.append(box((-16, 24, -22), (16, 34, 22), {"*": RIVETED, "up": LACQUER}))
    m.append(box((-16.5, 33, -22.5), (16.5, 35, 22.5), {"*": GILT}))
    m.append(box((-12, 26, 22), (12, 31, 22.5), {"*": LACQUER, "south": f"{FRIEZE}!"}))
    # Vision slits and rivet heads on the front.
    for x in (-10, 6):
        m.append(box((x, 29, 22.5), (x + 4, 30, 23), NUT))
    # The sloped glacis down to the nose.
    # The sloped front plate, in two halves a tile wide each, so every face's texture fits inside its sprite (a 32-pixel
    # face of a turned box would read past it).
    for x0 in (-16, 0):
        m.append(box((x0, 10, 28), (x0 + 16, 24, 34), {"*": RIVETED}, rotation=("x", -30, (0, 17, 31))))
    # Twin amber headlamps.
    for x in (-12, 10):
        m.append(box((x, 18, 33), (x + 2, 21, 34.5), {"*": BAND, "south": f"{AMBER}!"}))
    # The engine deck at the back: a perforated grille, two smokestacks with gilt bands. The stacks stop below the cannon,
    # which sweeps over them when the turret turns to the rear (down to 36.3 pixels at full depression and recoil).
    m.append(box((-14, 24, -31), (14, 27, -22), {"*": PERFORATED}))
    for x in (-8, 8):
        m += cyl("y", x, -27, 2.5, 27, STACK_TOP, EXHAUST, SOOT)
        m += cyl("y", x, -27, 3.1, STACK_TOP - 3.5, STACK_TOP - 2, GILT)
    m.append(box((-17.5, 5, -33), (17.5, 9, -31), {"*": SKID}))
    return m + side_frame(1) + side_frame(-1)


def turret():
    """The turret, about its own pivot: a lacquered drum with a brass ring, a copper-dome cupola and the mantlet. The
    ring reaches down a pixel to sit on the casemate's gilt band, so no slit shows under the turret."""
    m = []
    m += cyl("y", 0, 0, 11, 0, 8, RIVETED, LACQUER)
    m += cyl("y", 0, 0, 11.6, -1, 1.5, BRASS)
    m += cyl("y", 0, -4, 4.5, 8, 11, DOME)
    m.append(box((-5, 2, 8), (5, 10, 13), {"*": BRASS}))
    return m


def barrel():
    """The cannon, about the barrel's pivot, pointing along +z: a steel tube in a lacquered sleeve, ending in a brass
    muzzle band whose steel face holds the bore. The tube ends inside the band, so no faces share a plane; the sleeve
    starts a quarter pixel further back, so at full recoil its end stays clear of the mantlet's face."""
    m = []
    m += cyl("z", 0, 0, 2.2, 3, 34, TUBE)
    m += cyl("z", 0, 0, 3.0, 2.75, 9, LACQUER)
    m += cyl("z", 0, 0, 3.0, 32, 36, BRASS, TUBE)
    m.append(bore(36, 1.6, 3.0))
    return m


def link():
    """One track link along the path, centred on the origin: a tread plate with a raised grouser (one side; the
    renderer mirrors it to the other)."""
    w = TRACK_OUTER - TRACK_INNER + 1
    return [box((-w / 2, -1, -2.8), (w / 2, 1, 2.8), {"*": SKID, "down": TREAD}),
            box((-w / 2, -2, -0.75), (w / 2, -1, 0.75), {"*": SKID})]


def export():
    return {"landship_body": tiled_quads(body()), "landship_turret": tiled_quads(turret()),
            "landship_barrel": tiled_quads(barrel()), "landship_link": tiled_quads(link())}


def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"entity.{MOD}.landship"] = "Landship"
    lang[f"entity.{MOD}.cannon_shell"] = "Cannon Shell"
    lang[f"message.{MOD}.landship.fuel"] = "Fuel: %s%%"
    lang[f"message.{MOD}.landship.empty"] = "Out of fuel"
    lang[f"message.{MOD}.landship.full"] = "The fuel tank is full"
    lang[f"message.{MOD}.landship.refuelled"] = "Refuelled: %s%%"
    lang[f"message.{MOD}.landship.no_room"] = "Not enough room here for a Landship"
    lang[f"message.{MOD}.landship.no_shells"] = "No cannon shells"
    (assets / "landship_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    write(data / "recipe" / "landship.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["PCP", "EDE", "BGB"],
        "key": {"P": "#c:plates/steel", "C": f"{MOD}:imperial_crest", "E": f"{MOD}:diesel_engine",
                "D": "minecraft:dispenser", "B": f"{MOD}:belt", "G": "#c:gears/steel"},
        "result": {"id": f"{MOD}:landship", "count": 1}})
    write(data / "recipe" / "cannon_shell.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["P", "G", "N"],
        "key": {"P": "#c:plates/steel", "G": "minecraft:gunpowder", "N": "#c:nuggets/brass"},
        "result": {"id": f"{MOD}:cannon_shell", "count": 4}})


# ------------------------------------------------------------------ art

def tread():
    """The track's outer face: dark iron plates with a raised grouser bar and worn bright edges."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (44, 42, 40)
            if y % 8 in (3, 4):
                c = (86, 82, 76)
            elif y % 8 == 2:
                c = (120, 114, 104)
            elif y % 8 == 7:
                c = (26, 24, 24)
            if x in (0, 15):
                c = (34, 32, 30)
            img.putpixel((x, y), c + (255,))
    return img


def draw_all(save):
    save(tread(), "block", TREAD)
    save(item_icons.draw("landship"), "item", "landship")
    save(item_icons.draw("cannon_shell"), "item", "cannon_shell")
