"""The lairs (docs/features/witching-season.md, "The lairs: shared rules"; docs/features/hollow-acre.md): the numbers the
Java (src/main/java/.../lair) and the data (tools/lair_data.py) share. tools/check_mod_data.py checks the Java against
them.

Each lair is its own small pocket dimension, a void. A ritual opens an instance of it: a slot along the X axis,
SPACING blocks from the next, into which the lair's structure template is placed fresh. Players come in through the
ritual's gate (their own choice), cannot build or break anything there, and go home to exactly where they stood.
"""
FEATURE = "agriculture"

# Server options in config/jugcraft.properties (JugcraftConfig.TEXT_OPTIONS), with their defaults and (for numbers) the
# range the server keeps them in. lairs.off_season: "on" lets the rituals work all year at night; "off" only during the
# Halloween event (halloween.mode and its dates).
OPTIONS = {"lairs.instances": "8", "lairs.party_size": "4", "lairs.gate_seconds": "60", "lairs.off_season": "on"}
LIMITS = {"lairs.instances": (1, 32), "lairs.party_size": (1, 16), "lairs.gate_seconds": (10, 600)}

SPACING = 1024        # blocks between the instance slots of a lair, along X
BASE_Y = 64           # the template's lowest layer
CHECK_TICKS = 20      # the players in each lair are looked at once a second
EMPTY_SECONDS = 30    # an instance nobody has been inside for this long closes (its boss leaves, healed)
EDGE_DAMAGE = 4.0     # the mist's toll for flying or falling off, never below half a heart (1 health)

# The lair-only blocks: unbreakable fixtures, in the wither_immune and dragon_immune tags, with no item and no drops.
# The Hollow Acre's, the exit (every lair's), the Spindle Loft's (docs/features/spindle-loft.md), the Glacier Hall's
# (docs/features/glacier-hall.md), then the Cinder Kiln's (docs/features/cinder-kiln.md).
LAIR_BLOCKS = {
    "blighted_soil": "Blighted Soil",
    "black_wheat": "Black Wheat",
    "mown_stubble": "Mown Stubble",
    "lair_brazier": "Soul Brazier",
    "lair_moon": "Harvest Moon",
    "lair_exit": "Grey Mist",
    "doily_lace": "Doily Lace",
    "spool_wood": "Spool Wood",
    "spool_thread": "Spool Thread",
    "pincushion": "Pincushion",
    "pincushion_seam": "Pincushion Seam",
    "pincushion_leaf": "Pincushion Leaf",
    "needle_steel": "Needle Steel",
    "pin_shaft": "Pin",
    "measuring_tape": "Measuring Tape",
    "thimble_metal": "Thimble",
    "taut_thread": "Taut Thread",
    "grimy_skylight": "Grimy Skylight",
    "drift_snow": "Drift Snow",
    "trampled_snow": "Trampled Snow",
    "glare_ice": "Glare Ice",
    "giant_icicle": "Giant Icicle",
    "mammoth_tusk": "Mammoth Tusk",
    "frozen_hoard": "Frozen Hoard",
    "kiln_brick": "Kiln Brick",
    "cracked_basalt": "Cracked Basalt",
    "molten_slag": "Molten Slag",
    "trough_stone": "Trough Stone",
    "sluice_gate": "Sluice Gate",
}
# The colours of the Spindle Loft's thread: its four spools', and the white of the threads between them.
THREAD_COLOURS = ("green", "blue", "beige", "red", "white")
# The doily's four lace patterns, in rings: a solid band, an open mesh, a flower and the scalloped edge.
LACE_PATTERNS = 4
# The Glacier Hall's giant icicles are hung in parts, from the vault down: the base, the middle (as many as it is long)
# and the tip. Its trampled snow is a whole block or a half one (the snow ramp's half steps), by its height in halves.
ICICLE_PARTS = ("base", "middle", "tip")
TRAMPLED_HEIGHTS = (1, 2)
# Glare ice is as slick as blue ice: its friction (vanilla blocks have 0.6, ice 0.98, blue ice 0.989).
GLARE_FRICTION = 0.989
# The Cinder Kiln's molten slag burns whoever stands in it (but a fire-immune creature, or anyone under Fire
# Resistance): this much damage (hot floor, as a magma block's), and it sets them burning for this long.
SLAG = {"damage": 1.0, "burn_seconds": 3}
# Its sluice gates: each is its frame (posts and lintel), its panels and its wheel. Turned when ready, a sluice opens and
# floods its trough of trough stone for flood_ticks; then it closes, the trough drains, and it fills again for
# refill_ticks before it can be turned again. Using any block of a gate turns it. A gate's blocks are found by walking
# from the one used through the blocks of the gate touching it, its trough by walking from under the gate along the
# trough stone touching it: at most gate_blocks and trough_blocks of each.
SLUICE = {"flood_ticks": 200, "refill_ticks": 400, "gate_blocks": 40, "trough_blocks": 96}
SLUICE_PARTS = ("frame", "panel", "wheel")
SLUICE_FLOWS = ("ready", "open", "filling", "choked")   # choked: by the Cinder Tyrant's slag
# Fixtures a player in a lair may use (everything else is refused): the exits, the braziers Vesperine's fight lights, and
# the Cinder Kiln's sluice gates.
FIXTURES = ("lair_exit", "lair_brazier", "sluice_gate")

# The Last Rites (the Hollow Acre's ritual): a grave in the Overworld at night, lit candles round it, a Mourning Wreath
# laid on it and the Death Knell rung beside it.
RITE = {
    "grave_range": 4,          # how near the ringer the grave must be (blocks, a cube)
    "candle_range": 4,         # how near the grave the candles must be (blocks, a cube)
    "candles": 4,              # lit candles needed (a block of vanilla candles counts each candle)
    "wreath_range": 2,         # the wreath lies on the grave or beside it (blocks round the grave block found)
    "knell_cooldown": 40,      # ticks between rings of one bell
}
MOURNING_FLOWERS = ["jugcraft:black_rose", "jugcraft:funeral_lily", "jugcraft:spider_lily", "jugcraft:asphodel",
                    "jugcraft:snowdrop", "minecraft:lily_of_the_valley", "minecraft:wither_rose"]
# Any candle the séance counts, and the Chandlery's pillar candles.
RITE_CANDLES = ["#jugcraft:seance_candles", "jugcraft:black_pillar_candle", "jugcraft:ivory_pillar_candle"]

# A prick of the finger (the Spindle Loft's ritual): a Cursed Spindle used on a Spinning Wheel in the Overworld at
# night. The wheel spins wild and stays the gate for lairs.gate_seconds; whoever goes in wakes in the loft half-blind.
SPINDLE = {
    "waking_ticks": 40,        # Blindness on waking on the pincushion
}

# The Frost Horn's call (the Glacier Hall's ritual): blown in the Overworld at night, standing on snow or ice
# (#jugcraft:frost_horn_ground). A whirl of white mist opens in the snow at the blower's feet and stays the gate for
# lairs.gate_seconds; whoever goes in lands on the hall's ledge with frost on their skin. Not a Witching Season ritual:
# it works all year, whatever lairs.off_season says.
HORN = {
    "cooldown": 40,            # ticks between blows of one horn, called or refused
    "frost_ticks": 120,        # how frozen the arrival is (of the 140 that would freeze them through): frost, no harm
}
# What the horn may be blown standing on: vanilla's snow and ice, and Jugcraft's winter snow.
HORN_GROUND = ["minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow", "minecraft:ice", "minecraft:packed_ice",
               "minecraft:blue_ice", "minecraft:frosted_ice", "jugcraft:seasonal_snow"]

# The Kiln Seal (the Cinder Kiln's ritual): pressed into a magma block (#jugcraft:kiln_seal_ground), in the Nether or in
# a volcanic land of the Overworld (#jugcraft:kiln_seal_lands), at any hour. The magma cracks open and a vent of sparks
# and smoke rises from it, the gate for lairs.gate_seconds; whoever goes in sinks through the stone and lands on the
# kiln's ledge. Not a Witching Season ritual: it works all year, whatever lairs.off_season says.
SEAL = {
    "cooldown": 40,            # ticks between presses of one seal, opened or refused
    "vent_range": 3,           # how near another open vent into the kiln must not be (blocks)
}
SEAL_GROUND = ["minecraft:magma_block"]
SEAL_LANDS = ["jugcraft:volcano", "jugcraft:cinder_barrens"]

ITEMS = {
    "mourning_wreath": "Mourning Wreath",
    "death_knell": "Death Knell",
    "cursed_spindle": "Cursed Spindle",
    "frost_horn": "Frost Horn",
    "kiln_seal": "Kiln Seal",
}
GATE = {"entity": "mist_gate", "display": "Mist Gate", "width": 1.6, "height": 2.6}

RECIPES = {
    # Four mourning flowers round a vine.
    "mourning_wreath": {"pattern": [" F ", "FVF", " F "], "key": {"F": "#jugcraft:mourning_flowers", "V": "minecraft:vine"}},
    # A handbell: a gold bell, an iron clapper, a bone handle. Not used up by ringing.
    "death_knell": {"pattern": ["G", "N", "B"], "key": {"G": "minecraft:gold_ingot", "N": "minecraft:iron_nugget",
                                                       "B": "minecraft:bone"}},
    # A spindle: thread and two spider eyes over a gold whorl set with amethyst, on a stick between two more threads.
    # Used up by its ritual.
    "cursed_spindle": {"pattern": ["ESE", "GAG", "STS"], "key": {
        "E": "minecraft:spider_eye", "S": "minecraft:string", "G": "minecraft:gold_ingot",
        "A": "minecraft:amethyst_shard", "T": "minecraft:stick"}},
    # A goat horn bound with gold and leather, packed with snow. Used up by its call.
    "frost_horn": {"pattern": ["LGL", "SHS", " G "], "key": {
        "L": "minecraft:leather", "G": "minecraft:gold_ingot", "S": "minecraft:snow_block", "H": "minecraft:goat_horn"}},
    # A disc of four obsidian round a magma block, marked with two blaze powder and rimmed with two gold ingots. Used up
    # by its pressing.
    "kiln_seal": {"pattern": ["BOB", "OMO", "GOG"], "key": {
        "B": "minecraft:blaze_powder", "O": "minecraft:obsidian", "M": "minecraft:magma_block", "G": "minecraft:gold_ingot"}},
}

# Each lair's dimension: a void with its own biome; fixed time, no weather, no beds or respawn anchors. Its sky, fog and
# light colours, its music, and the motes drifting in its air; and what a player is told on coming in.
LAIRS = {
    "hollow_acre": {
        "display": "The Hollow Acre",
        "enter": "You step through the mist into %s",
        "sky": "#0b0910", "fog": "#3a2a44", "water_fog": "#1d1622", "water": "#3f3150",
        "ambient_light_color": "#2a2030", "sky_light_color": "#3a3050",
        "music": "minecraft:music.nether.soul_sand_valley",
        "ambient_light": 0.15,
        "motes": ("minecraft:white_ash", 0.006),
    },
    "spindle_loft": {
        "display": "The Spindle Loft",
        "enter": "You prick your finger, fall asleep and wake in %s",
        "sky": "#0d0a08", "fog": "#3b3024", "water_fog": "#1e1913", "water": "#3d342a",
        "ambient_light_color": "#3a2e22", "sky_light_color": "#4a3c2c",
        "music": "minecraft:music.overworld.deep_dark",
        "ambient_light": 0.2,
        "motes": ("minecraft:white_ash", 0.004),
    },
    "glacier_hall": {
        "display": "The Glacier Hall",
        "enter": "The snow gives way under you, and you fall into %s",
        "sky": "#c6dcea", "fog": "#9fbdd2", "water_fog": "#4f7d9c", "water": "#3f6a8a",
        "ambient_light_color": "#7c98b8", "sky_light_color": "#a8c4dc",
        "music": "minecraft:music.overworld.frozen_peaks",
        "ambient_light": 0.25,
        "motes": ("minecraft:snowflake", 0.008),
    },
    "cinder_kiln": {
        "display": "The Cinder Kiln",
        "enter": "The magma swallows you, and you sink into %s",
        "sky": "#2c120b", "fog": "#4a1d10", "water_fog": "#2a3a44", "water": "#3f6a8a",
        "ambient_light_color": "#6e3418", "sky_light_color": "#8a4420",
        "music": "minecraft:music.nether.basalt_deltas",
        "ambient_light": 0.2,
        "motes": ("minecraft:ash", 0.012),
    },
}


def blocks():
    """Every block the lairs register: the lair-only blocks and the Mourning Wreath (the Cursed Spindle, the Frost Horn and
    the Kiln Seal are items)."""
    return list(LAIR_BLOCKS) + ["mourning_wreath"]


def itemless_blocks():
    """The lair-only blocks have no item: nobody can carry a piece of a lair home."""
    return list(LAIR_BLOCKS)


def items():
    return list(ITEMS)
