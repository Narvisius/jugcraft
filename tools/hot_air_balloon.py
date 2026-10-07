"""The hot-air balloon fiesta (fall addition 29): hot-air balloons that rise on a burner's heat and go where the winds
take them, winds that blow different ways at different heights, pibals to find them, and mooring posts to tether a
balloon for rides. The numbers here are what agriculture/HotAirBalloon, FiestaWinds, Pibal and MooringPostBlock use;
tools/check_mod_data.py compares them. tools/hot_air_balloon_data.py writes the JSON and the quads the client draws
them from, and tools/hot_air_balloon_textures.py the textures.

A balloon is a wicker basket under a burner and an envelope (three designs; the Jack-o'-Lantern is a special shape).
Placed from its item on open ground, it stands cold and limp-ready: anyone can climb in (up to `riders`), and the first
aboard is its pilot. The pilot holds jump to fire the burner, which heats the envelope by `fire` a tick and burns a fuel
unit; the envelope cools by `cool` a tick, and holding back opens the vent at the crown, cooling it by `vent` more. Above
`neutral` heat it rises, below it sinks, at `climb` blocks a tick for each unit of heat over or under, but never faster
than `max_climb` up or `max_sink` down; the higher it is, the thinner the air (`thin` blocks above sea level take away
a unit of lift). It eases towards that speed (`response` of the way a tick). It can't be steered: aloft, it drifts with
the wind at its height (`drift` of the way a tick); on the ground it stays put.

Fuel: the burner burns what a generator burns (machine/GeneratorFuels: coal, charcoal, coke, a coal block), a unit for
every `fuel_per_burn_tick` ticks a generator would burn it, up to `max_fuel` units. Using fuel on a balloon loads one.

Winds (FiestaWinds): `layers` layers, `layer` blocks deep from sea level up, each with its own direction and speed for
the day: `base` blocks a tick at the bottom, `per_layer` more for each layer up. The lowest two blow roughly opposite
ways (`box_spread` degrees either side of opposite), so a pilot can go out low and come back higher: the "box" that
fiestas are known for. Through the day every layer swings `sway` degrees either way. In rain the winds blow `storm`
times as hard. Each day's winds come from the world's seed.

A pibal (a pilot balloon) is let go from the hand: it rises `rise` blocks a tick, drifting with the winds, so its path
shows the layers; it pops after `life` ticks.

A Mooring Post tethers a balloon: using it ties the nearest balloon within `reach` blocks (or unties the one it holds).
A tethered balloon goes no further than `rope` blocks across from the post and `height` blocks above it.

Advancements: Up, Up and Away (fly `up_height` blocks over the ground), The Box (land within `box_home` blocks of
where you took off, having been `box_away` blocks away), Mass Ascension (be aloft, `aloft` blocks up, with `crowd`
other balloons aloft within `crowd_range` blocks).
"""
import math

HOT_AIR_BALLOON = {
    "entity": "hot_air_balloon", "entity_display": "Hot-Air Balloon", "riders": 4,
    # The basket, in blocks: its width and height; the burner's height over its floor, and the envelope's mouth (the
    # throat), its height, and its widest radius.
    "basket": 1.5, "basket_height": 1.125, "burner": 2.25, "throat": 3.25, "envelope_height": 11.0, "envelope_radius": 4.5,
    # Heat (0 to 1) a tick: firing, cooling, venting; the heat that floats it; blocks a tick of climb a unit of heat
    # gives, and the fastest up and down; blocks above sea level that cost a unit of lift; how fast it answers.
    "fire": 0.004, "cool": 0.0008, "vent": 0.006, "neutral": 0.5, "climb": 0.5, "max_climb": 0.25, "max_sink": 0.15,
    "thin": 400, "response": 0.04, "drift": 0.02,
    # Fuel: a generator's burn ticks for each unit, and the most it carries (in units, a tick of burner each).
    "fuel_per_burn_tick": 4, "max_fuel": 12000,
    # The pilot's gauges every few ticks.
    "gauge_ticks": 10,
}

WINDS = {"layer": 16, "layers": 8, "base": 0.05, "per_layer": 0.015, "box_spread": 30, "sway": 30, "storm": 1.5}

MOORING = {"block": "mooring_post", "display": "Mooring Post", "reach": 10.0, "rope": 8.0, "height": 16.0}

PIBAL = {"item": "pibal", "display": "Pibal", "entity": "pibal", "entity_display": "Pibal", "rise": 0.12, "life": 600, "count": 4}

BURNER = {"item": "balloon_burner", "display": "Balloon Burner"}

ADVANCEMENT_RULES = {"up_height": 32, "box_home": 16, "box_away": 64, "aloft": 8, "crowd": 2, "crowd_range": 128}

# The three designs: their items and names, and what goes in the middle of the recipe.
KINDS = {
    "harvest": {"item": "harvest_balloon", "display": "Harvest Stripes Balloon", "key": "minecraft:orange_dye"},
    "pumpkin": {"item": "pumpkin_balloon", "display": "Jack-o'-Lantern Balloon", "key": "minecraft:carved_pumpkin"},
    "moon": {"item": "harvest_moon_balloon", "display": "Harvest Moon Balloon", "key": "minecraft:blue_dye"},
}

ADVANCEMENTS = {
    "up_up_and_away": {"icon": "jugcraft:harvest_balloon", "title": "Up, Up and Away",
                       "description": "Fly a hot-air balloon 32 blocks above the ground", "frame": "task"},
    "the_box": {"icon": "jugcraft:pibal", "title": "The Box",
                "description": "Fly a balloon out on one wind and home on another, landing near where you took off", "frame": "challenge"},
    "mass_ascension": {"icon": "jugcraft:pumpkin_balloon", "title": "Mass Ascension",
                       "description": "Fly a hot-air balloon with two others aloft nearby", "frame": "goal"},
}

# The burner is copper coils over an iron valve, lit by a flint and steel. An envelope is wool sewn round its design,
# over the burner and a bamboo basket. A mooring post is an iron bollard on stone with a lead. Pibals are slime-rubber
# balloons on string.
SHAPED = [
    {"id": BURNER["item"], "pattern": ["CFC", "CIC"],
     "key": {"C": "minecraft:copper_ingot", "F": "minecraft:flint_and_steel", "I": "minecraft:iron_ingot"},
     "result": BURNER["item"], "count": 1, "category": "misc"},
    {"id": MOORING["block"], "pattern": [" L ", "III", "SSS"],
     "key": {"L": "minecraft:lead", "I": "minecraft:iron_ingot", "S": "minecraft:stone"},
     "result": MOORING["block"], "count": 1, "category": "building"},
] + [
    {"id": spec["item"], "pattern": ["WDW", "WBW", "KKK"],
     "key": {"W": "#minecraft:wool", "D": spec["key"], "B": "jugcraft:" + BURNER["item"], "K": "minecraft:bamboo"},
     "result": spec["item"], "count": 1, "category": "misc"}
    for spec in KINDS.values()
]
SHAPELESS = [
    {"id": PIBAL["item"], "inputs": ["minecraft:slime_ball", "minecraft:string"], "result": PIBAL["item"], "count": PIBAL["count"],
     "category": "misc"},
]


def items():
    return [k["item"] for k in KINDS.values()] + [BURNER["item"], PIBAL["item"]] + blocks()


def blocks():
    return [MOORING["block"]]


def displays():
    out = {k["item"]: k["display"] for k in KINDS.values()}
    out[BURNER["item"]] = BURNER["display"]
    out[PIBAL["item"]] = PIBAL["display"]
    out[MOORING["block"]] = MOORING["display"]
    return out


# ---------------------------------------------------------------- the envelope's shape

def profile(s):
    """The envelope's radius (blocks) at `s` of the way up it, from its throat (0) to its crown (1): a cone flaring from
    the throat, swelling to its widest at 0.62 of the way, and rounding over to the crown."""
    r0, rmax, knee = 1.0, HOT_AIR_BALLOON["envelope_radius"], 0.62
    if s <= knee:
        t = s / knee
        return r0 + (rmax - r0) * math.sin(t * math.pi / 2) ** 1.25
    t = (s - knee) / (1 - knee)
    return rmax * math.sqrt(max(0.0, 1 - t * t))


def pumpkin_profile(s):
    """The Jack-o'-Lantern's radius at `s`: a skirt from the throat to a round, squat pumpkin, flattened a little at its
    top round its stem."""
    r0, rmax = 1.0, HOT_AIR_BALLOON["envelope_radius"] * 1.05
    skirt = 0.18
    if s <= skirt:
        return r0 + (rmax * 0.62 - r0) * (s / skirt)
    t = (s - skirt) / (1 - skirt)
    # A sphere squashed to 0.82 of its width, its bottom joined to the skirt.
    a = math.pi * (0.16 + 0.84 * t)
    return max(rmax * math.sin(a) * (1 - 0.12 * t ** 6), 0.0 if t >= 1 else 0.05)
