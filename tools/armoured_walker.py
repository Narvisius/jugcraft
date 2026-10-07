"""The Armoured Walker (batch 58, docs/features/armoured-walker.md), after the owner's own Blender model: a squat,
heavily armoured two-legged walker.

- A tall hull of riveted blue-grey plate, its corners and top edges chamfered so it reads as an octagon, with a framed
  gun port in the front carrying the hull cannon, an amber lamp either side, and a sandbag pouch on the roof.
- A heavy chain slung across the front between two brackets, one end hanging loose; a slatted skirt below.
- On its right a drum hatch and a short jointed arm ending in a riveted tool head; on its left a piston arm that rams
  forward.
- Two stocky legs: a flat armour slab over each thigh, an angled shin, and a broad foot plate on a hinge.

The pilot walks it as the Diesel Walker (its controls and fuel), holds use to fire the hull cannon and presses attack
to ram with the piston arm. The raiders field it too, in their paint (raiders/RaiderWalker).

Exported as quads (assets/jugcraft/armoured_walker_quads.json) for client/ArmouredWalkerRenderer and
client/RaiderWalkerRenderer, which swing the legs and drive the piston. Units are pixels, the walker facing +z with the
ground at y = 0. tools/check_mod_data.py keeps the joints and numbers the same as the Java.
"""
import json

from PIL import Image

import clean_metal
from steampunk_models import box, cyl
from zeppelin import tiled_quads

MOD = "jugcraft"

# Movement and fuel are the Diesel Walker's. The cannon: ticks between shots and the shell's speed; the ram: damage,
# how hard it throws, its reach in blocks and ticks between rams.
CANNON_COOLDOWN = 40
CANNON_SPEED = 2.4
RAM_DAMAGE = 16
RAM_KNOCKBACK = 1.6
RAM_REACH = 2.6
RAM_COOLDOWN = 24
HEALTH = 90
WIDTH = 2.6
HEIGHT = 4.6

ITEMS = {"armoured_walker": "Armoured Walker"}
TOOLTIPS = {"armoured_walker": "A heavily armoured walker with a hull cannon and a piston ram. Walk with the movement "
                               "keys, hold use to fire the cannon (Heavy Shells) and press attack to ram. Refuel "
                               "with a diesel or kerosene bucket."}

PLATE, SEAM, DARK, LEG = "aw_plate", "aw_plate_seam", "aw_plate_dark", "aw_leg"
CHAIN, LAMP, BORE, CANVAS = "aw_chain", "aw_lamp", "aw_bore", "aw_canvas"
GUNMETAL, CHROME = "dp_gunmetal", "dp_chrome"

# Joints, in pixels: the hips, the right arm's shoulder and the piston's head at rest. Keep in sync with the renderers.
HIPS = {"left": (10, 27, 0), "right": (-10, 27, 0)}
SHOULDER = (-24, 56, 1)
PISTON = (24, 49, 6)
# How far the piston rams forward, in pixels.
PISTON_STROKE = 10
# Where the cannon's muzzle is, in pixels.
MUZZLE = (0, 56, 29)


def hull():
    """The hull, its gun port and cannon, lamps, chain, roof pouch and the slatted skirt below."""
    m = []
    # The octagonal hull: a core box with the four vertical corners cut back, and a chamfered roof.
    m.append(box((-15, 36, -16), (15, 68, 16), {"*": PLATE, "north": SEAM, "south": SEAM}))
    m.append(box((-19, 36, -12), (-15, 68, 12), PLATE))
    m.append(box((15, 36, -12), (19, 68, 12), PLATE))
    for x0, x1 in ((-18, -15), (15, 18)):
        for z0, z1 in ((-15, -12), (12, 15)):
            m.append(box((x0, 37, z0), (x1, 67, z1), DARK))
    m.append(box((-16, 68, -13), (16, 71, 13), {"*": PLATE, "up": DARK}))
    m.append(box((-13, 71, -10), (13, 72, 10), DARK))
    # A belt of plate round the bottom of the hull, 0.25 px below the hull's own underside so the two never share a
    # plane (they are drawn in different textures).
    m.append(box((-15.5, 35.75, -16.5), (15.5, 40, 16.5), SEAM))
    m.append(box((-19.5, 35.75, -12.5), (19.5, 40, 12.5), SEAM))
    # The gun port: a raised frame round a dark recess, the cannon's box mount and barrel.
    m.append(box((-7, 48, 16), (7, 67, 17.5), {"*": DARK, "south": PLATE}))
    m.append(box((-5, 50, 16.5), (5, 65, 17.6), {"*": BORE}))
    m.append(box((-7.5, 66, 15.5), (7.5, 68, 18.5), GUNMETAL))
    m.append(box((-4, 52, 16), (4, 60, 21), {"*": GUNMETAL, "south": DARK}))
    # The bore stands 0.1 px proud of the muzzle ring, so the dark bore and the ring's gunmetal face never share a plane.
    m += cyl("z", 0, 56, 2.6, 21, 29.1, GUNMETAL, BORE)
    m += cyl("z", 0, 56, 3.1, 27.5, 29, GUNMETAL)
    # Lamp housings either side of the port (the glass is drawn full bright in lamps()).
    for x in (-10, 10):
        m.append(box((x - 1.5, 54, 16), (x + 1.5, 58, 17), GUNMETAL))
    # The chain brackets at the front corners and the chain itself, sagging between them, one end hanging.
    for x in (-16, 16):
        m.append(box((x - 1.5, 37, 16), (x + 1.5, 43, 19), GUNMETAL))
    # Links alternate face-on (wide, flat) and edge-on (narrow, deep), as a real chain's do.
    for i in range(15):
        t = i / 14.0
        x = -14 + 28 * t
        y = 40 - 3.5 * (1 - (2 * t - 1) ** 2)
        if i % 2:
            m.append(box((x - 0.6, y - 0.6, 18.0), (x + 0.6, y + 0.6, 19.6), CHAIN))
        else:
            m.append(box((x - 1.3, y - 0.9, 18.5), (x + 1.3, y + 0.9, 19.1), CHAIN))
    for j in range(5):
        y = 38.5 - j * 1.7
        if j % 2:
            m.append(box((10.4, y - 0.8, 18.2), (11.6, y + 0.8, 19.6), CHAIN))
        else:
            m.append(box((10.1, y - 0.9, 18.6), (11.9, y + 0.9, 19.2), CHAIN))
    # The sandbag pouch on the roof.
    m.append(box((-14, 71, 3), (-5, 75, 11), CANVAS))
    m.append(box((-13, 75, 4), (-6, 76, 10), CANVAS))
    # The skirt: a dark core under the hull, slats hanging in front and behind.
    m.append(box((-14, 27, -11), (14, 36, 11), DARK))
    for x in range(-12, 13, 4):
        m.append(box((x - 1, 25, 11), (x + 1, 36, 12.5), LEG))
        m.append(box((x - 1, 25, -12.5), (x + 1, 36, -11), LEG))
    # The drum hatch on the walker's right side (-x), where the tool arm joins.
    m += cyl("x", 56, 1, 7, -21, -19, DARK, PLATE)
    m += cyl("x", 56, 1, 4, -22.5, -21, GUNMETAL, CHROME)
    return m


def lamps():
    """The two amber lamps, drawn at full brightness."""
    return [box((x - 1, 55, 17), (x + 1, 57, 17.6), {"*": LAMP}) for x in (-10, 10)]


def leg():
    """One leg below its hip: the hip joint, a flat armour slab over the thigh, the knee, an angled shin, the ankle
    housing and a broad hinged foot."""
    m = []
    # The hip drum stops 0.25 px short of the hull's skirt plane (x = +-14 at the hips), so its caps never flicker against it.
    m += cyl("x", 0, 0, 3.5, -3.75, 3.75, GUNMETAL, DARK)
    m.append(box((-2.5, -15, -3), (2.5, 0, 3), GUNMETAL))
    # The thigh slab: a broad riveted plate hanging in front of the thigh from just under the hull.
    m.append(box((-6.5, -17, 3), (6.5, 6, 5.5), {"*": LEG, "south": PLATE}))
    m.append(box((-7, 4, 2.5), (7, 6.5, 6), GUNMETAL))
    m.append(box((-3.5, -19, -3.5), (3.5, -14, 4), LEG))
    m.append(box((-3, -25, -6), (3, -16, 0), LEG, ("x", -22.5, [0, -16, 0])))
    m.append(box((-4, -24, -3), (4, -20, 5), {"*": LEG, "south": PLATE}))
    m.append(box((-6.5, -27, -7), (6.5, -25, 10), {"*": LEG, "up": DARK}))
    m.append(box((-4.25, -25, -2), (4.25, -23, 7), GUNMETAL))
    m += cyl("x", -24, 8, 1.2, -3.5, 3.5, GUNMETAL, DARK)
    return m


def tool_arm():
    """The right arm, from its shoulder: an axle out of the drum, a shoulder block with two pins, a forearm and a
    riveted tool head with a short barrel."""
    m = []
    m += cyl("x", 0, 0, 2.5, 0, 4, GUNMETAL, CHROME)
    m.append(box((-8, -6, -4), (0, 6, 4), {"*": PLATE, "west": DARK}))
    for y in (-4, 4):
        m += cyl("z", -4, y, 0.8, 4, 6, CHROME)
    m.append(box((-6, -15, -1.5), (-3, -6, 1.5), GUNMETAL))
    m.append(box((-8, -21, -3), (-1, -15, 6), {"*": PLATE, "south": DARK}))
    m += cyl("z", -4.5, -18, 1.3, 6, 9, GUNMETAL, BORE)
    return m


def piston_base():
    """The left arm's mount on the hull side and the two piston cylinders."""
    m = [box((19, 44, -3), (24, 54, 5), {"*": DARK, "east": PLATE})]
    for y in (46.5, 51.5):
        m += cyl("z", 22, y, 1.6, 0, 8, GUNMETAL, CHROME)
    return m


def piston_head():
    """The piston rods and ram head, from its rest position (PISTON); the renderer slides it forward to ram."""
    m = []
    for y in (-2.5, 2.5):
        m += cyl("z", -2, y, 0.8, -6, 2, CHROME)
    m.append(box((-4, -5, 2), (1, 5, 5), {"*": PLATE, "south": DARK}))
    m.append(box((-4.5, -5.5, 4.5), (1.5, 5.5, 5.5), GUNMETAL))
    return m


def parts():
    return {"armoured_walker_hull": hull(), "armoured_walker_lamps": lamps(), "armoured_walker_leg": leg(),
            "armoured_walker_tool_arm": tool_arm(), "armoured_walker_piston_base": piston_base(),
            "armoured_walker_piston_head": piston_head()}


def export():
    return {name: tiled_quads(elements) for name, elements in parts().items()}


# ------------------------------------------------------------------ data

def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"entity.{MOD}.armoured_walker"] = "Armoured Walker"
    # Written compact: it is thousands of quads.
    (assets / "armoured_walker_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    # An upgrade of the Diesel Walker: armour plate all round, a steel block for the cannon and pistons for the ram.
    write(data / "recipe" / "armoured_walker.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["PSP", "PWP", "R R"],
        "key": {"P": "#c:plates/steel", "S": f"{MOD}:steel_block", "W": f"{MOD}:diesel_walker", "R": "minecraft:piston"},
        "result": {"id": f"{MOD}:armoured_walker", "count": 1}})


# ------------------------------------------------------------------ art (the clean style, tools/clean_metal.py)

STEEL = [(44, 52, 58), (58, 68, 76), (74, 86, 94), (90, 104, 112), (112, 126, 134), (140, 152, 158)]
NAVY = [(34, 40, 48), (44, 52, 62), (56, 66, 76), (70, 80, 90), (86, 96, 106)]
RUST = [(92, 58, 38), (122, 78, 48), (150, 100, 62)]
BROWN = [(58, 54, 48), (74, 70, 62), (92, 86, 76), (112, 106, 94), (132, 126, 112)]


def riveted(palette, seam=False, rust=True):
    """A riveted plate in the owner's model's blue-grey: a bevelled panel with a rivet near each corner and one midway
    along each edge, kept quiet; with a seam, a horizontal joint across the middle. Rust only as two warm touches on
    the bottom edge."""
    img = clean_metal.canvas(palette[2])
    clean_metal.bevel(img, 0, 0, 15, 15, palette[3], palette[1], palette[2])
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13), (2, 7), (13, 7)):
        clean_metal.put(img, x, y, palette[-1])
        clean_metal.put(img, x + 1, y + 1, palette[1])
    if seam:
        for x in range(1, 15):
            clean_metal.put(img, x, 7, palette[1])
            clean_metal.put(img, x, 8, palette[3])
    if rust:
        for x, y in ((5, 15), (6, 15), (11, 15)):
            clean_metal.put(img, x, y, RUST[0])
        clean_metal.put(img, 5, 14, RUST[1])
    return img


def chain():
    """Dark iron chain with a little rust: the links' faces lit along the top."""
    img = clean_metal.canvas((58, 50, 44))
    for x in range(16):
        clean_metal.put(img, x, 0, (86, 72, 60))
        clean_metal.put(img, x, 1, (74, 62, 52))
        clean_metal.put(img, x, 15, (40, 34, 30))
    for x, y in ((3, 6), (4, 7), (11, 9), (12, 10)):
        clean_metal.put(img, x, y, RUST[0])
    return img


def lamp():
    img = clean_metal.canvas((226, 160, 48))
    clean_metal.rect(img, 0, 0, 15, 15, (150, 96, 20))
    clean_metal.rect(img, 3, 3, 12, 12, (255, 214, 120))
    clean_metal.rect(img, 5, 5, 10, 10, (255, 240, 190))
    return img


def bore():
    img = clean_metal.canvas((18, 20, 24))
    clean_metal.rect(img, 0, 0, 15, 1, (36, 40, 46))
    return img


def canvas_bag():
    img = clean_metal.canvas((118, 108, 84))
    for y in range(16):
        for x in range(16):
            if (x + 2 * y) % 7 == 0:
                clean_metal.put(img, x, y, (100, 92, 72))
    for x in range(16):
        clean_metal.put(img, x, 8, (88, 80, 62))
    return img


def icon():
    """The item: the walker from the front, hull, gun port, chain and legs."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def rect(x0, y0, x1, y1, c):
        clean_metal.rect(img, x0, y0, x1, y1, c)
    rect(4, 1, 11, 8, STEEL[3])
    rect(3, 2, 12, 7, STEEL[3])
    rect(4, 1, 11, 1, STEEL[4])
    rect(6, 2, 9, 6, NAVY[1])
    rect(7, 4, 8, 5, (18, 20, 24))
    rect(5, 4, 5, 4, (255, 200, 90))
    rect(10, 4, 10, 4, (255, 200, 90))
    for x in range(4, 12):
        clean_metal.put(img, x, 8 if x in (4, 11) else 9 if x in (5, 6, 9, 10) else 10, RUST[1])
    rect(1, 4, 2, 6, STEEL[2])
    rect(13, 4, 14, 5, NAVY[2])
    rect(5, 9, 10, 10, NAVY[0])
    rect(4, 11, 6, 13, BROWN[2])
    rect(9, 11, 11, 13, BROWN[2])
    rect(3, 14, 7, 14, BROWN[1])
    rect(8, 14, 12, 14, BROWN[1])
    return img


def draw_all(save):
    save(riveted(STEEL), "block", PLATE)
    save(riveted(STEEL, seam=True), "block", SEAM)
    save(riveted(NAVY, rust=False), "block", DARK)
    save(riveted(BROWN), "block", LEG)
    save(chain(), "block", CHAIN)
    save(lamp(), "block", LAMP)
    save(bore(), "block", BORE)
    save(canvas_bag(), "block", CANVAS)
    save(icon(), "item", "armoured_walker")
