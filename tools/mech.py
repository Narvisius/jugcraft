"""The Diesel Walker (batch 47, docs/features/diesel-walker.md): a rideable dieselpunk mech.

A squat walker about four blocks tall in the look of the batch 44 giants: chipped red plating over rusted, riveted
iron, barrel shoulders, an open chest cockpit the pilot sits in, a glowing amber core in the chest, an engine block
with twin smokestacks on its back, a huge riveted fist on its left arm and a spinning mining drill on its right.

The pilot walks it with the movement keys (it climbs one-block steps by itself and jumps with the jump key), holds
use to drill the block they look at (one block at a time, as a pilot breaking it by hand would, honouring protected
land) and presses attack to punch. It burns diesel or kerosene poured in from buckets.

Java: walker/ (DieselWalker, DieselWalkerItem, WalkerInputPayload, JugcraftWalkers); client/DieselWalkerRenderer
animates the parts exported here (assets/jugcraft/walker_quads.json). tools/check_mod_data.py keeps the numbers the
same.
"""
import json
import math

from PIL import Image

import item_icons
from steampunk_models import box, cyl
from zeppelin import tiled_quads

MOD = "jugcraft"

# Movement, in blocks a tick and degrees a tick.
WALK_SPEED = 0.2
TURN = 4.0
JUMP = 0.6
# The drill: how far it reaches, ticks per unit of a block's hardness (never fewer than DRILL_MIN_TICKS), and the
# hardest block it will cut (obsidian is 50; bedrock and other unbreakable blocks never).
DRILL_REACH = 5.0
DRILL_TICKS_PER_HARDNESS = 5
DRILL_MIN_TICKS = 3
DRILL_MAX_HARDNESS = 50
# The fist: damage, how hard it throws what it hits (blocks a tick) and ticks between punches.
PUNCH_DAMAGE = 12
PUNCH_KNOCKBACK = 1.0
PUNCH_COOLDOWN = 16
# Fuel in mB: the tank, what a bucket adds and what walking or drilling burns a second.
FUEL_TANK = 4000
FUEL_PER_BUCKET = 1000
FUEL_PER_SECOND = 4
HEALTH = 60
WIDTH = 2.5
HEIGHT = 4.25

ITEMS = {"diesel_walker": "Diesel Walker"}
TOOLTIPS = {"diesel_walker": "A diesel mech with a drill arm and a big fist. Walk with the movement keys, jump with "
                             "jump, hold use to drill and attack to punch. Refuel with a diesel or kerosene bucket."}

RUST, RUST_BARE, RED, BAND, SKID = "dr_rust", "dr_rust_bare", "dr_red", "dr_band", "dr_skid"
PATINA, PERFORATED, RIB_RUST, GRATE = "dr_patina", "dr_perforated", "dr_ribbed_rust", "dr_grate"
COPPER, NUT, BLUE, EXHAUST, SOOT = "dr_copper_pipe", "dr_nut", "dr_blue", "dp_exhaust", "dr_soot"
AMBER, DRILL_BIT = "dr_amber_on", "dp_drill_bit"

# Joints, in pixels in the walker's own space (facing +z, the ground at y = 0). Keep in sync with
# client/DieselWalkerRenderer.
HIPS = {"left": (8, 26, 0), "right": (-8, 26, 0)}
SHOULDERS = {"fist": (21, 46, 0), "drill": (-21, 46, 0)}
# The drill bit's hub, from the drill arm's shoulder.
BIT = (0, -16, 12)
# How the renderer swings them, in degrees: the legs swing up to LEG_SWING either way, the arms follow them by ARM_FOLLOW,
# the fist punches PUNCH_SWING forward and the drill arm is raised DRILL_RAISE to drill. tools/gun_poses.py poses the
# walker over these.
LEG_SWING, ARM_FOLLOW, PUNCH_SWING, DRILL_RAISE = 28, 0.3, 70, 20


def body():
    """Pelvis, chest with the open cockpit, barrel shoulders, back engine and stacks."""
    m = []
    m.append(box((-12, 20, -7), (12, 30, 7), {"*": RUST, "up": GRATE}))
    m.append(box((-12.5, 21, -7.5), (12.5, 22.5, 7.5), BAND))
    # The chest: a front wall with the core, side walls and a tall backrest; the pilot sits on the floor between them.
    m.append(box((-16, 30, 6), (16, 40, 10), {"*": RED}))
    m.append(box((-16.5, 39, 5.5), (16.5, 41, 10.5), BAND))
    # The side walls stand a quarter pixel proud of the backrest's sides, back and foot, so the two never share a plane.
    for x0, x1 in ((-16, -12), (12, 16)):
        m.append(box((x0 - 0.25 if x0 < 0 else x0, 29.75, -10.25), (x1 + 0.25 if x1 > 0 else x1, 48, 10.25), RED))
        m.append(box((x0 - 0.5, 44, -10.5), (x1 + 0.5, 45.5, 10.5), BAND))
    m.append(box((-16, 30, -10), (16, 54, -6), {"*": RUST, "north": RUST_BARE}))
    m.append(box((-12, 30, -6), (12, 31, 6), {"*": RUST, "up": GRATE}))
    m.append(box((-14, 54, -9), (14, 56, -6), BAND))
    # Rivets down the front corners and a hazard-red visor bar above the cockpit.
    for x in (-14, 13):
        for y in (32, 35, 38):
            m.append(box((x, y, 10), (x + 1, y + 1, 10.5), NUT))
    # The engine on the back: perforated patina housing, copper lines, twin smokestacks and blue caps.
    m.append(box((-12, 32, -18), (12, 54, -10), {"*": PATINA, "south": PERFORATED, "north": PERFORATED}))
    for x in (-7, 7):
        m += cyl("y", x, -14, 2.5, 54, 68, EXHAUST, SOOT)
        m += cyl("y", x, -14, 3.1, 60, 61.5, BAND)
    m.append(box((-13, 36, -19), (-11, 50, -17), COPPER))
    m.append(box((11, 36, -19), (13, 50, -17), COPPER))
    m += cyl("z", 0, 44, 2.5, -20, -18, BLUE)
    # Barrel shoulders over each arm.
    for x0, x1 in ((14, 28), (-28, -14)):
        m += cyl("x", 50, 0, 8, x0, x1, RIB_RUST, RUST_BARE)
        for x in (x0 + 2, x1 - 3.5):
            m += cyl("x", 50, 0, 8.6, x, x + 1.5, BAND)
    return m


def core():
    """The glowing amber core in the chest front, drawn at full brightness."""
    return [box((-4, 31, 10), (4, 39, 10.75), {"*": BAND, "south": f"{AMBER}!"})]


def leg():
    """One leg below its hip: thigh, knee, shin and a heavy foot pointing forward. The thigh stops a quarter pixel inside
    the pelvis's side planes (which it swings across), and the knee drum stands a quarter pixel proud of the shin."""
    m = [box((-3.75, -12, -4), (3.75, 0, 4), SKID)]
    m += cyl("x", -12, 0, 4.5, -5.25, 5.25, NUT, BAND)
    m.append(box((-5, -22, -5), (5, -13, 5), {"*": RUST}))
    m.append(box((-5.5, -16, -5.5), (5.5, -15, 5.5), BAND))
    m.append(box((-6, -26, -7), (6, -22, 9), {"*": RED, "down": SKID}))
    m.append(box((-6.5, -25.75, 7), (6.5, -24, 10), BAND))
    return m


def fist_arm():
    """The left arm: upper arm, forearm and a huge riveted fist held forward."""
    m = [box((-3, -12, -3), (3, 0, 3), SKID)]
    m.append(box((-5, -20, -4), (5, -12, 8), RUST))
    m.append(box((-7, -27, 6), (7, -13, 19), {"*": RED}))
    for y in (-24, -20, -16):
        m.append(box((-7.5, y, 18.5), (7.5, y + 1.5, 19.5), BAND))
    for x in (-5, -1, 3):
        m.append(box((x, -26, 19), (x + 2, -14, 20), NUT))
    return m


def drill_arm():
    """The right arm: upper arm, forearm and the drill housing (the bit is drawn and spun separately)."""
    m = [box((-3, -12, -3), (3, 0, 3), SKID)]
    m.append(box((-5, -20, -4), (5, -12, 8), PATINA))
    m += cyl("z", 0, -16, 5.25, 6, 12, BAND, RUST_BARE)
    m.append(box((-6, -13, 0), (6, -11.5, 8), COPPER))
    return m


def drill_bit():
    """The drill bit, a stepped cone along z from its hub at the origin."""
    m = []
    for r, z0, z1 in ((4.5, 0, 5), (3.5, 5, 10), (2.5, 10, 15), (1.5, 15, 19), (0.75, 19, 22)):
        m += cyl("z", 0, 0, r, z0, z1, DRILL_BIT)
    return m


def export():
    return {"walker_body": tiled_quads(body()), "walker_core": tiled_quads(core()), "walker_leg": tiled_quads(leg()),
            "walker_fist_arm": tiled_quads(fist_arm()), "walker_drill_arm": tiled_quads(drill_arm()),
            "walker_drill_bit": tiled_quads(drill_bit())}


def write_all(write, assets, data, lang, condition):
    for item, name in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = name
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{item}"}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item}"}})
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"entity.{MOD}.diesel_walker"] = "Diesel Walker"
    lang[f"message.{MOD}.walker.fuel"] = "Fuel: %s%%"
    lang[f"message.{MOD}.walker.empty"] = "Out of fuel"
    lang[f"message.{MOD}.walker.full"] = "The fuel tank is full"
    lang[f"message.{MOD}.walker.refuelled"] = "Refuelled: %s%%"
    lang[f"message.{MOD}.walker.no_room"] = "Not enough room here for a Diesel Walker"
    (assets / "walker_quads.json").write_text(json.dumps(export(), separators=(",", ":")) + "\n", encoding="utf-8")
    write(data / "recipe" / "diesel_walker.json", {
        "fabric:load_conditions": condition("machines"), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["RER", "DPA", "L L"],
        "key": {"R": f"{MOD}:riveted_rust_plate", "E": f"{MOD}:diesel_engine", "D": f"{MOD}:mining_drill",
                "P": "#c:plates/steel", "A": "minecraft:anvil", "L": "minecraft:piston"},
        "result": {"id": f"{MOD}:diesel_walker", "count": 1}})


# ------------------------------------------------------------------ art

def draw_all(save):
    save(item_icons.draw("diesel_walker"), "item", "diesel_walker")
