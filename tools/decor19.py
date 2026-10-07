"""Halloween decorations batch 19: the Laboratory, the Larder and the Dining Room (docs/features/laboratory-larder-dining.md),
prop sets 11 to 15 of the Witching Season plan (docs/features/witching-season.md), drawn from the owner's reference
pictures. Models and textures: tools/decor19_data.py, sculpted on the toolkit in tools/flora_art.py.

11. The Reanimation Rig: the Lightning Harness (agriculture/LightningHarnessBlock) fires on a strong redstone pulse
    (`pulse` or more), when a running Tesla Coil within `coil_range` arcs, or when lightning strikes within
    `strike_reach` blocks above it, and wakes the patient of a Lab Table up to `table_reach` blocks below for
    `wake_ticks`; the Brain-Vat Console (an analogue memory cell) and the Crawling Hand.
12. The Spider's Larder: the Silk Cocoon (a `slots`-slot larder hanging from a ceiling), the Egg Sac Cluster (faces like
    glow lichen), the Web Drape (2 x 2, slowing by SLOW) and the Silk Spool Stack (three spools in any dye colour).
13. The Poltergeist's Dinner Party: the Haunted Dining Chair (slides out at night for a player within `reach`), the
    Floating Table Setting (SETTINGS) and the Grandfather Clock (hands, moon dial, chimes, an hourly pulse).
14. The Witchlight Lantern Path: three lamps that wake (WITCHLIGHT) as a player comes near, in five glass colours.
15. The Yard Silhouette (FIGURES, sixteen turns) and the Harvest Moon Lamp (2 x 2, its face the moon's phase).
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

HARNESS = {"block": "lightning_harness", "display": "Lightning Harness", "pulse": 13, "coil_range": 8, "strike_reach": 4,
           "table_reach": 4, "wake_ticks": 100, "check_ticks": 5, "arc_ticks": 12, "cooldown_ticks": 20}
CONSOLE = {"block": "brain_vat_console", "display": "Brain-Vat Console"}
HAND = {"block": "crawling_hand", "display": "Crawling Hand", "lap_ticks": 80}

COCOON = {"block": "silk_cocoon", "display": "Silk Cocoon", "slots": 9, "wriggle_ticks": 20}
EGG_SACS = {"block": "egg_sac_cluster", "display": "Egg Sac Cluster"}
# A Web Drape slows whoever walks through it by these factors (a cobweb's are 0.25, 0.05, 0.25).
DRAPE = {"block": "web_drape", "display": "Web Drape", "slow": [0.6, 0.75, 0.6]}
SPOOLS = {"block": "silk_spool_stack", "display": "Silk Spool Stack", "spools": 3, "colours": ["white", "purple", "red"]}

CHAIR = {"block": "haunted_dining_chair", "display": "Haunted Dining Chair", "seat": 0.5, "reach": 2.0, "check_ticks": 20,
         "out_ticks": 100, "slide": 6.0}
SETTING = {"block": "floating_table_setting", "display": "Floating Table Setting", "light": 7}
SETTINGS = ["dinner", "tea", "feast"]
CLOCK = {"block": "grandfather_clock", "display": "Grandfather Clock", "pulse_ticks": 4, "hour_ticks": 1000, "face_ticks": 200,
         "strike_gap": 20}

WITCHLIGHTS = {"witchlight_lamp_post": "Witchlight Lamp-Post", "witchlight_path_stake": "Witchlight Path Stake",
               "hanging_witchlight": "Hanging Witchlight"}
WITCHLIGHT = {"asleep": 3, "awake": 14, "range": 6.0, "linger_ticks": 200, "fade_ticks": 40, "check_ticks": 10}
WITCHLIGHT_COLOURS = ["purple", "green", "orange", "blue", "red"]

SILHOUETTE = {"block": "yard_silhouette", "display": "Yard Silhouette"}
FIGURES = ["arched_cat", "prowling_cat", "witch", "bats", "wolf", "crow"]
MOON = {"block": "harvest_moon_lamp", "display": "Harvest Moon Lamp", "light": 15}

MESSAGES = {"container.jugcraft.silk_cocoon": "Silk Cocoon",
            "message.jugcraft.table_setting": "The table is laid for %s",
            "setting.jugcraft.dinner": "dinner", "setting.jugcraft.tea": "tea", "setting.jugcraft.feast": "a feast",
            "message.jugcraft.silhouette": "Now: %s",
            "figure.jugcraft.arched_cat": "an arched cat", "figure.jugcraft.prowling_cat": "a prowling cat",
            "figure.jugcraft.witch": "a witch on her broom", "figure.jugcraft.bats": "a flock of bats",
            "figure.jugcraft.wolf": "a howling wolf", "figure.jugcraft.crow": "a crow",
            "message.jugcraft.brain_vat": "It remembers %s"}


def blocks():
    return ([HARNESS["block"], CONSOLE["block"], HAND["block"], COCOON["block"], EGG_SACS["block"], DRAPE["block"],
             SPOOLS["block"], CHAIR["block"], SETTING["block"], CLOCK["block"]] + list(WITCHLIGHTS)
            + [SILHOUETTE["block"], MOON["block"]])


def items():
    return blocks()


def names():
    out = {d["block"]: d["display"] for d in (HARNESS, CONSOLE, HAND, COCOON, EGG_SACS, DRAPE, SPOOLS, CHAIR, SETTING, CLOCK,
                                               SILHOUETTE, MOON)}
    out.update(WITCHLIGHTS)
    return out


SHAPED = [
    {"id": HARNESS["block"], "pattern": ["CRC", "B B", "C C"], "key": {"C": "minecraft:copper_ingot", "R": "minecraft:lightning_rod",
                                                                      "B": "jugcraft:brass_ingot"},
     "result": HARNESS["block"], "count": 1, "category": "redstone"},
    {"id": CONSOLE["block"], "pattern": ["GGG", "RBR", "PCP"], "key": {"G": "minecraft:glass", "R": "minecraft:redstone",
                                                                      "B": "minecraft:rotten_flesh",
                                                                      "P": "jugcraft:brass_ingot", "C": "minecraft:comparator"},
     "result": CONSOLE["block"], "count": 1, "category": "redstone"},
    {"id": COCOON["block"], "pattern": ["S", "W", "S"], "key": {"S": "minecraft:string", "W": "minecraft:white_wool"},
     "result": COCOON["block"], "count": 1, "category": "building"},
    {"id": DRAPE["block"], "pattern": ["CC", "CC"], "key": {"C": "minecraft:cobweb"},
     "result": DRAPE["block"], "count": 1, "category": "building"},
    {"id": SPOOLS["block"], "pattern": ["WWW", "PPP"], "key": {"W": "#minecraft:wool", "P": "#minecraft:planks"},
     "result": SPOOLS["block"], "count": 1, "category": "building"},
    {"id": CHAIR["block"], "pattern": ["P  ", "PWP", "S S"], "key": {"P": "minecraft:dark_oak_planks", "W": "minecraft:red_wool",
                                                                    "S": "minecraft:stick"},
     "result": CHAIR["block"], "count": 2, "category": "building"},
    {"id": CLOCK["block"], "pattern": ["PGP", "PCP", "PNP"], "key": {"P": "minecraft:dark_oak_planks", "G": "minecraft:glass",
                                                                    "C": "minecraft:clock", "N": "minecraft:gold_ingot"},
     "result": CLOCK["block"], "count": 1, "category": "redstone"},
    {"id": "witchlight_lamp_post", "pattern": ["IN ", "IG ", "I  "], "key": {"I": "minecraft:iron_ingot", "N": "minecraft:gold_nugget",
                                                                            "G": "minecraft:purple_stained_glass"},
     "result": "witchlight_lamp_post", "count": 1, "category": "building"},
    {"id": "witchlight_path_stake", "pattern": ["N", "G", "S"], "key": {"N": "minecraft:gold_nugget", "G": "minecraft:purple_stained_glass",
                                                                       "S": "minecraft:stick"},
     "result": "witchlight_path_stake", "count": 2, "category": "building"},
    {"id": "hanging_witchlight", "pattern": ["C", "N", "G"], "key": {"C": "minecraft:iron_chain", "N": "minecraft:gold_nugget",
                                                                    "G": "minecraft:purple_stained_glass"},
     "result": "hanging_witchlight", "count": 1, "category": "building"},
    {"id": SILHOUETTE["block"], "pattern": ["P", "D", "S"], "key": {"P": "#minecraft:planks", "D": "minecraft:black_dye",
                                                                   "S": "minecraft:stick"},
     "result": SILHOUETTE["block"], "count": 2, "category": "building"},
    {"id": MOON["block"], "pattern": ["WGW", "GWG", "NNN"], "key": {"W": "minecraft:white_wool", "G": "minecraft:glowstone",
                                                                   "N": "minecraft:gold_ingot"},
     "result": MOON["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": HAND["block"], "inputs": ["minecraft:rotten_flesh", "minecraft:bone"], "result": HAND["block"], "count": 1,
     "category": "building"},
    {"id": EGG_SACS["block"], "inputs": ["minecraft:string", "minecraft:spider_eye"], "result": EGG_SACS["block"], "count": 3,
     "category": "building"},
    {"id": SETTING["block"], "inputs": ["minecraft:bowl", "minecraft:glass_bottle", "minecraft:iron_nugget", "minecraft:candle"],
     "result": SETTING["block"], "count": 1, "category": "building"},
]
