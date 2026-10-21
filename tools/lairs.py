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
# (docs/features/glacier-hall.md), the Cinder Kiln's (docs/features/cinder-kiln.md), the Lantern Fen's
# (docs/features/lantern-fen.md), the Archive of Last Names' (docs/features/crypt-archive.md), the Foundry Trench's
# (docs/features/foundry-trench.md), then the Moonlit Ring's (docs/features/moonlit-ring.md).
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
    "fen_lamp": "Fen Lamp",
    "name_stone": "Name-Stone",
    "grave_seal": "Grave Seal",
    "archive_lectern": "Lectern of Last Names",
    "phylactery": "Phylactery",
    "pylon_lattice": "Pylon Lattice",
    "pylon_crown": "Pylon Crown",
    "discharge_breaker": "Breaker",
    "ground_terminal": "Ground Terminal",
    "moonwood_leaves": "Moonwood Leaves",
    "warden_bell": "Warden Bell",
    "raked_turf": "Raked Turf",
    "red_mist": "Red Mist",
    "star_stone": "Star Stone",
    "star_dial": "Star Dial",
    "star_door": "Star Door",
    "dust_vent": "Dust Vent",
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
# Fixtures a player in a lair may use (everything else is refused): the exits, the braziers Vesperine's fight lights, the
# Cinder Kiln's sluice gates, the Lantern Fen's lamps, the Archive of Last Names' grave seals and lectern, the Foundry
# Trench's breakers, and the Moonlit Ring's Warden Bells.
FIXTURES = ("lair_exit", "lair_brazier", "sluice_gate", "fen_lamp", "grave_seal", "archive_lectern", "discharge_breaker",
            "warden_bell", "red_mist", "star_dial")

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

# The Hag Stone (the Lantern Fen's ritual): looked through at night in the Overworld, standing in shallow water or on mud
# (#jugcraft:hag_stone_ground) in a wetland (#jugcraft:hag_stone_lands). Through its hole a lantern burns far off in mist;
# the water stills and mist rises from it with a marsh-light in it, the gate for lairs.gate_seconds; whoever goes in wakes
# on the fen's jetty. Not a Witching Season ritual: it works all year, at night, whatever lairs.off_season says.
STONE = {
    "cooldown": 40,            # ticks between lookings through one stone, answered or not
    "mist_range": 3,           # how near another open mist into the fen must not be (blocks)
}
# What may be stood on instead of shallow water: mud, in it or on it.
STONE_GROUND = ["minecraft:mud", "minecraft:muddy_mangrove_roots"]
# The wetlands: every swamp (#c:is_swamp, Jugcraft's own and vanilla's), the Floodplain and the Muskeg.
STONE_LANDS = ["#c:is_swamp", "minecraft:swamp", "minecraft:mangrove_swamp", "jugcraft:floodplain", "jugcraft:muskeg"]

# The Lantern Fen's lamps: lit, they burn at this light level; one gone out is lit again by using it.
FEN_LAMP = {"light": 15}

# The Writ of Exhumation (the Archive of Last Names' ritual): served on a stone sarcophagus (#jugcraft:writ_tombs) in the
# Overworld, in a crypt: under a roof, no sky to be seen from the cell over its lid. Any hour, all year. The lid grinds
# aside (the sarcophagus's own open state), and soul-fire and grey mist rise from it, the gate for lairs.gate_seconds;
# whoever goes in wakes on the archive's landing. Then the lid grinds shut.
WRIT = {
    "cooldown": 40,            # ticks between servings of one writ, answered or not
    "mist_range": 3,           # how near another open mist into the archive must not be (blocks)
}
WRIT_TOMBS = ["jugcraft:stone_brick_sarcophagus", "jugcraft:deepslate_sarcophagus", "jugcraft:blackstone_sarcophagus"]
# The archive's fixtures. The Lich's three names, each with its rune; a name-stone is dark, lit (the one he answers to)
# or erased (scraped blank; read back at the lectern), and gives this light lit. A grave seal is whole or broken. The
# phylactery is whole or cracked, and gives this light whole.
NAMES = {"sexton": "spade", "crowned": "crown", "scribe": "quill"}
NAME_GLOWS = ("dark", "lit", "erased")
NAME_STONE = {"light": 9}
PHYLACTERY = {"light": 10}
LECTERN = {"cooldown": 20}     # ticks between readings of the lectern by one player

# Sealed Orders (the Foundry Trench's ritual): used on a Field Telephone in the Overworld that stands in a trench: under
# the open sky, with at least `trench_blocks` trench-works blocks (#jugcraft:trench_works) within `trench_range` blocks of
# it (a cube). Any hour, all year. The bell rings and a whistle shrills down the line, the gate for lairs.gate_seconds;
# whoever goes in wakes in the trench's dugout. Then the line goes dead.
ORDERS = {
    "cooldown": 40,            # ticks between uses of one set of orders, answered or not
    "line_range": 3,           # how near another open line into the trench must not be (blocks)
    "trench_range": 3,         # how far round the telephone the trench is looked for (blocks, a cube)
    "trench_blocks": 4,        # trench-works blocks needed round it
    "bell_ticks": 40,          # ticks between rings of an open line's bell
}
TRENCH_WORKS = ["jugcraft:sandbags", "jugcraft:sandbags_slab", "jugcraft:sandbags_stairs", "jugcraft:timber_revetment",
                "jugcraft:timber_revetment_slab", "jugcraft:timber_revetment_stairs", "jugcraft:duckboard"]
# The trench's fixtures. Three pylons, each with its breaker; a pylon is dark or live (its crown burning, its breaker's
# lamp lit, the one the Dreadnought's charge will strike), and its terminal live while it earths a discharge. A breaker
# is open or thrown, and fouled while a plate jams it. Light: a live crown, a live breaker's lamp, a live terminal.
PYLON_NAMES = ("west", "east", "south")
PYLON_CROWN = {"light": 12}
BREAKER = {"light": 10, "rest": 10}   # its lamp's light when live; the least ticks between one player's uses
TERMINAL = {"light": 8}

# The Howling Totem (the Moonlit Ring's ritual): used at night in the Overworld, in werewolf country (the werewolves'
# haunts, #jugcraft:werewolf_haunts), under the open sky, standing on woodland floor (earth a tree could grow on: grass,
# dirt, podzol, moss and the like), and no nearer growing or potted wolfsbane than the werewolves' ward reach. Any night,
# not only a full moon's, all year. The totem howls and something answers; it is used up, and Grey Mist rises where it
# stood, the gate for lairs.gate_seconds; whoever goes in comes to in the ring's camp.
TOTEM = {
    "cooldown": 40,            # ticks between uses of one totem, answered or not
    "mist_range": 3,           # how near another open mist into the ring must not be (blocks)
}
# The ring's Warden Bells (west, north and east): a bell rung swings for swing_ticks, and cannot be rung again until it
# has stopped.
WARDEN_BELL = {"swing_ticks": 40}
BELL_NAMES = ("west", "north", "east")
# The Altar Keystone (the Cydonian Sanctum's rite, docs/features/cydonian-sanctum.md): set into the Ancient Altar of
# Adam-ondi-Ahman (tools/adam_ondi_ahman.py) at night, in the Overworld. The three altars under it kindle with red fire
# one after another, kindle_ticks apart, and a column of red light rises; then the red mist opens over the altar, the gate
# for lairs.gate_seconds, and whoever set the keystone steps through onto Mars, if still within reach_blocks of it. The
# keystone crumbles to dust. Not a Witching Season ritual: it works all year, at night, whatever lairs.off_season says.
KEYSTONE = {
    "cooldown": 40,            # ticks between settings of one keystone, answered or not
    "mist_range": 3,           # how near another open mist (or a kindling) into the Sanctum must not be (blocks)
    "kindle_ticks": 20,        # between one altar kindling and the next (three altars, then the mist)
    "reach_blocks": 8,         # how near the altar the one who set the keystone must still be to be taken through
}
# The altars under the Ancient Altar, as tools/adam_ondi_ahman.py stacks them: each one's top, blocks under the Ancient
# Altar, and its half-width; the lowest first, the order they kindle in.
KEYSTONE_ALTARS = ((3, 5), (2, 3), (1, 1))

ITEMS = {
    "mourning_wreath": "Mourning Wreath",
    "death_knell": "Death Knell",
    "cursed_spindle": "Cursed Spindle",
    "frost_horn": "Frost Horn",
    "kiln_seal": "Kiln Seal",
    "hag_stone": "Hag Stone",
    "exhumation_writ": "Writ of Exhumation",
    "sealed_orders": "Sealed Orders",
    "howling_totem": "Howling Totem",
    "altar_keystone": "Altar Keystone",
}
GATE = {"entity": "mist_gate", "display": "Mist Gate", "width": 1.6, "height": 2.6}

# Each recipe is named for its file; it makes its "result" (the recipe's own name if none), and loads only when the
# lairs' switch and its "features" are on.
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
    # A flint with a hole worn through it, threaded on string, bound with two gold ingots and two blocks of mud, and set
    # with a fermented spider eye. Used up by its looking.
    "hag_stone": {"pattern": ["MSM", "GFG", " E "], "key": {
        "M": "minecraft:mud", "S": "minecraft:string", "G": "minecraft:gold_ingot", "F": "minecraft:flint",
        "E": "minecraft:fermented_spider_eye"}},
    # A writ: three sheets of paper over two bones and a bottle of ectoplasm (from a restless spirit), signed with an ink
    # sac. Used up by its serving.
    "exhumation_writ": {"pattern": ["PPP", "BEB", " I "], "key": {
        "P": "minecraft:paper", "B": "minecraft:bone", "E": "jugcraft:ectoplasm", "I": "minecraft:ink_sac"}},
    # Orders: three sheets of paper over a steel plate and a coil of copper wire, sealed with a honeycomb's wax. Used up by
    # the call.
    "sealed_orders": {"pattern": ["PPP", "SHW"], "key": {
        "P": "minecraft:paper", "S": "jugcraft:steel_plate", "H": "minecraft:honeycomb", "W": "jugcraft:copper_wire"}},
    # A totem: a werewolf's pelt (any kind) wrapped round three bones and bound with two silver ingots. Used up by its
    # howl.
    "howling_totem": {"pattern": ["SPS", " B ", "BB "], "key": {
        "S": "#c:ingots/silver", "P": "#jugcraft:werewolf_pelts", "B": "minecraft:bone"}},
    # The Altar Keystone, two ways, so neither specialty is forced through the other. The factories': a wedge of chiseled
    # stone bound in titanium and tungsten, a red star of redstone set in it and an advanced circuit to wake it. Used up
    # by its setting.
    "altar_keystone": {"pattern": ["TWT", "RSR", " C "], "key": {
        "T": "jugcraft:titanium_ingot", "W": "jugcraft:tungsten_ingot", "R": "minecraft:redstone",
        "S": "minecraft:chiseled_stone_bricks", "C": "jugcraft:advanced_circuit"},
        "features": ["adam_ondi_ahman", "titanium", "tungsten", "machines"]},
    # The Concordance's: the same stone set with dreamglass brought back from dreams (the Dreamwalkers' work) round a red
    # star, and the Starwatchers' astrolabe to read the sky by.
    "altar_keystone_from_dreamglass": {"pattern": ["DAD", "RSR", " D "], "key": {
        "D": "jugcraft:dreamglass", "A": "jugcraft:astrolabe", "R": "minecraft:redstone",
        "S": "minecraft:chiseled_stone_bricks"},
        "result": "altar_keystone", "features": ["adam_ondi_ahman", "concordance"]},
}
# What counts as a werewolf's pelt for the totem: every kind's (tools/agriculture.py WEREWOLF kinds), and the Alpha's
# own (tools/alpha_werewolf.py), one for one.
WEREWOLF_PELTS = ["jugcraft:werewolf_pelt", "jugcraft:snow_werewolf_pelt", "jugcraft:shadow_werewolf_pelt",
                  "jugcraft:alpha_pelt"]

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
    "lantern_fen": {
        "display": "The Lantern Fen",
        "enter": "The mist closes round you, and you wake in %s",
        "sky": "#0b120f", "fog": "#4a5a4c", "water_fog": "#26301f", "water": "#4c5a36",
        "ambient_light_color": "#2c3a2e", "sky_light_color": "#3c4c3e",
        "music": "minecraft:music.overworld.swamp",
        "ambient_light": 0.2,
        "motes": ("minecraft:glow", 0.002),
    },
    "crypt_archive": {
        "display": "The Archive of Last Names",
        "enter": "You go down the stair under the lid, and come to %s",
        "sky": "#06080c", "fog": "#1c2430", "water_fog": "#141a22", "water": "#2c3a4a",
        "ambient_light_color": "#26344a", "sky_light_color": "#30405a",
        "music": "minecraft:music.nether.soul_sand_valley",
        "ambient_light": 0.18,
        "motes": ("minecraft:white_ash", 0.004),
    },
    "foundry_trench": {
        "display": "The Foundry Trench",
        "enter": "A whistle shrills down the line, and you come to in %s",
        "sky": "#3a2a20", "fog": "#6a5442", "water_fog": "#2a2a24", "water": "#4a4a3c",
        "ambient_light_color": "#6a5038", "sky_light_color": "#8a6644",
        "music": "minecraft:music.nether.basalt_deltas",
        "ambient_light": 0.25,
        "motes": ("minecraft:ash", 0.008),
    },
    "moonlit_ring": {
        "display": "The Moonlit Ring",
        "enter": "The totem howls, something far off answers, and you come to in %s",
        "sky": "#0b1222", "fog": "#1c2638", "water_fog": "#141c2a", "water": "#2a3a52",
        "ambient_light_color": "#2c3a58", "sky_light_color": "#6a7aa0",
        "music": "minecraft:music.overworld.forest",
        "ambient_light": 0.2,
        "motes": ("minecraft:white_ash", 0.003),
    },
    "cydonian_sanctum": {
        "display": "The Cydonian Sanctum",
        "enter": "The red mist takes you, and you step out onto Mars, into %s",
        "sky": "#c9945a", "fog": "#b07548", "water_fog": "#5a3a28", "water": "#7a5238",
        "ambient_light_color": "#a8603a", "sky_light_color": "#d89a62",
        "music": "minecraft:music.overworld.badlands",
        "ambient_light": 0.3,
        "motes": ("minecraft:crimson_spore", 0.004),
    },
}


def blocks():
    """Every block the lairs register: the lair-only blocks and the Mourning Wreath (the Cursed Spindle, the Frost Horn, the
    Kiln Seal, the Hag Stone, the Writ of Exhumation, Sealed Orders and the Howling Totem are items)."""
    return list(LAIR_BLOCKS) + ["mourning_wreath"]


def itemless_blocks():
    """The lair-only blocks have no item: nobody can carry a piece of a lair home."""
    return list(LAIR_BLOCKS)


def items():
    return list(ITEMS)
