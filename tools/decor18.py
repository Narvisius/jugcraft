"""Halloween decorations, batch 18: the Crypt and the Ossuary (docs/features/crypt-and-ossuary.md), prop sets 6 to 10 of
the Witching Season plan (docs/features/witching-season.md), drawn from the owner's reference pictures. Models and
textures: tools/decor18_data.py, sculpted on the toolkit in tools/flora_art.py.

6. Iron-Bound Coffin (agriculture/IronBoundCoffinBlock): a Coffin with iron bands and `slots` slots, locked to a
   Skeleton Key (agriculture/SkeletonKeyItem) cut from a Key Blank the first time it locks one; a cut key copies onto
   blanks in a crafting grid (the special recipe jugcraft:key_copying). The Coffin Wardrobe stands upright and swaps
   the four armour pieces you wear with the four it holds.
7. Stone Sarcophagus (agriculture/SarcophagusBlock) in three stones, a tomb-chest of `slots` slots whose lid the
   Stonemason's Chisel recarves as one of LIDS; at night, shut, it knocks (KNOCK).
8. Bone Throne, Ribcage Bookcase (a chiseled bookshelf that powers enchanting tables), Skull Footstool and Vertebra
   Floor Lamp.
9. The Buried Colossus: a 2 x 2 x 2 Colossal Skull whose jaw drops on a redstone signal, Colossal Ribs that meet as an
   arch (RIB), Colossal Vertebrae that line up on any axis and a Colossal Femur two blocks long.
10. Gargoyle Sentinel (a hostile-mob sensor, SENTINEL), Gargoyle Rainspout (fills a cauldron below it in the rain,
    RAINSPOUT) and Chimera Finial (a weather sensor, FINIAL).
Every recipe follows the agriculture feature switch.
"""

FEATURE = "agriculture"

COFFIN = {"block": "iron_bound_coffin", "display": "Iron-Bound Coffin", "slots": 54}
KEY = {"item": "skeleton_key", "display": "Skeleton Key", "blank": "key_blank", "blank_display": "Key Blank",
       "recipe": "key_copying", "max_copies": 8}
WARDROBE = {"block": "coffin_wardrobe", "display": "Coffin Wardrobe"}

STONES = {"stone_brick": {"display": "Stone Brick Sarcophagus", "stone": "minecraft:stone_bricks"},
          "deepslate": {"display": "Deepslate Sarcophagus", "stone": "minecraft:polished_deepslate"},
          "blackstone": {"display": "Blackstone Sarcophagus", "stone": "minecraft:polished_blackstone"}}
SARCOPHAGUS = {"slots": 27, "open_ticks": 12}
# The lid's carvings, in the order the chisel turns them.
LIDS = ["plain", "knight", "lady", "skull"]
# A shut sarcophagus knocks at night: checked once a second (`check_ticks`), one chance in `chance` while a player is
# within `range` blocks, at most once every `cooldown_ticks`; three knocks `gap_ticks` apart.
KNOCK = {"range": 6.0, "check_ticks": 20, "chance": 30, "cooldown_ticks": 1200, "knocks": 3, "gap_ticks": 10}

THRONE = {"block": "bone_throne", "display": "Bone Throne", "seat": 0.6, "check_ticks": 20}
BOOKCASE = {"block": "ribcage_bookcase", "display": "Ribcage Bookcase"}
FOOTSTOOL = {"block": "skull_footstool", "display": "Skull Footstool", "seat": 0.4}
LAMP = {"block": "vertebra_floor_lamp", "display": "Vertebra Floor Lamp", "light": 13}

# The Colossal Skull's sockets ({x0, y0, x1, y1}, pixels from its first block), for the client's night glow: a sheet facing
# north at `glow_z`, 0.1 in front of the dark hollow closing the back of the sockets (its face at `hollow_z`).
SKULL = {"block": "colossal_skull", "display": "Colossal Skull", "jaw_degrees": 28.0, "jaw_speed": 2.0,
         "sockets": [(-10.0, 11.6, -2.6, 19.4), (2.6, 11.6, 10.0, 19.4)], "hollow_z": 8.0, "glow_z": 7.9}
RIB = {"block": "colossal_rib", "display": "Colossal Rib", "span": 2}
VERTEBRA = {"block": "colossal_vertebra", "display": "Colossal Vertebra"}
FEMUR = {"block": "colossal_femur", "display": "Colossal Femur"}

SENTINEL = {"block": "gargoyle_sentinel", "display": "Gargoyle Sentinel", "range": 16.0, "near": 2.0, "check_ticks": 10,
            "turn_speed": 6.0, "max_turn": 80.0}
RAINSPOUT = {"block": "gargoyle_rainspout", "display": "Gargoyle Rainspout", "reach": 4, "fill_ticks": 200, "check_ticks": 20,
             "stream": 8}
FINIAL = {"block": "chimera_finial", "display": "Chimera Finial", "rain": 7, "storm": 15, "check_ticks": 20}

MESSAGES = {"message.jugcraft.coffin.locked_to": "Locked. Only its key will open it now.",
            "message.jugcraft.coffin.unlocked": "Unlocked.",
            "message.jugcraft.coffin.wrong_key": "This key doesn't fit.",
            "message.jugcraft.coffin.key_cut": "The blank is cut to this lock: it is its key now.",
            "item.jugcraft.skeleton_key.wards": "Wards: %s",
            "item.jugcraft.key_blank.hint": "Sneak-use on an Iron-Bound Coffin to cut it to that lock",
            "container.jugcraft.iron_bound_coffin": "Iron-Bound Coffin",
            "container.jugcraft.sarcophagus": "Sarcophagus",
            "message.jugcraft.sarcophagus.effigy": "The lid is recarved: %s",
            "effigy.jugcraft.plain": "a plain lid", "effigy.jugcraft.knight": "a knight", "effigy.jugcraft.lady": "a lady",
            "effigy.jugcraft.skull": "a skull and crossbones"}


def sarcophagi():
    return [f"{stone}_sarcophagus" for stone in STONES]


def blocks():
    return ([COFFIN["block"], WARDROBE["block"]] + sarcophagi()
            + [THRONE["block"], BOOKCASE["block"], FOOTSTOOL["block"], LAMP["block"],
               SKULL["block"], RIB["block"], VERTEBRA["block"], FEMUR["block"],
               SENTINEL["block"], RAINSPOUT["block"], FINIAL["block"]])


def items():
    return blocks() + [KEY["blank"], KEY["item"]]


def names():
    out = {COFFIN["block"]: COFFIN["display"], WARDROBE["block"]: WARDROBE["display"], THRONE["block"]: THRONE["display"],
           BOOKCASE["block"]: BOOKCASE["display"], FOOTSTOOL["block"]: FOOTSTOOL["display"], LAMP["block"]: LAMP["display"],
           SKULL["block"]: SKULL["display"], RIB["block"]: RIB["display"], VERTEBRA["block"]: VERTEBRA["display"],
           FEMUR["block"]: FEMUR["display"], SENTINEL["block"]: SENTINEL["display"], RAINSPOUT["block"]: RAINSPOUT["display"],
           FINIAL["block"]: FINIAL["display"]}
    out.update({f"{stone}_sarcophagus": info["display"] for stone, info in STONES.items()})
    return out


SHAPED = [
    {"id": COFFIN["block"], "pattern": [" I ", "ICI", " I "], "key": {"I": "minecraft:iron_ingot", "C": "jugcraft:coffin"},
     "result": COFFIN["block"], "count": 1, "category": "building"},
    {"id": KEY["blank"], "pattern": ["N", "I"], "key": {"N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"},
     "result": KEY["blank"], "count": 2, "category": "equipment"},
    {"id": THRONE["block"], "pattern": ["SBS", "BWB", "BOB"], "key": {"S": "minecraft:skeleton_skull", "B": "minecraft:bone",
                                                                      "W": "minecraft:red_wool", "O": "minecraft:bone_block"},
     "result": THRONE["block"], "count": 1, "category": "building"},
    {"id": BOOKCASE["block"], "pattern": ["BPB", "B B", "BPB"], "key": {"B": "minecraft:bone", "P": "#minecraft:planks"},
     "result": BOOKCASE["block"], "count": 1, "category": "building"},
    {"id": FOOTSTOOL["block"], "pattern": ["W", "S", "B"], "key": {"W": "minecraft:red_wool", "S": "minecraft:skeleton_skull",
                                                                   "B": "minecraft:bone"},
     "result": FOOTSTOOL["block"], "count": 1, "category": "building"},
    {"id": LAMP["block"], "pattern": ["G", "B", "O"], "key": {"G": "minecraft:glowstone", "B": "minecraft:bone_block",
                                                              "O": "minecraft:bone"},
     "result": LAMP["block"], "count": 1, "category": "building"},
    {"id": SKULL["block"], "pattern": ["OOO", "OOO", "OOO"], "key": {"O": "minecraft:bone_block"},
     "result": SKULL["block"], "count": 1, "category": "building"},
    {"id": RIB["block"], "pattern": [" O", "O ", "O "], "key": {"O": "minecraft:bone_block"},
     "result": RIB["block"], "count": 2, "category": "building"},
    {"id": VERTEBRA["block"], "pattern": ["B B", "OOO", "B B"], "key": {"O": "minecraft:bone_block", "B": "minecraft:bone"},
     "result": VERTEBRA["block"], "count": 3, "category": "building"},
    {"id": FEMUR["block"], "pattern": ["OOO"], "key": {"O": "minecraft:bone_block"},
     "result": FEMUR["block"], "count": 1, "category": "building"},
    {"id": SENTINEL["block"], "pattern": [" S ", "SES", "RSR"], "key": {"S": "minecraft:stone_bricks", "E": "minecraft:ender_eye",
                                                                        "R": "minecraft:redstone"},
     "result": SENTINEL["block"], "count": 1, "category": "redstone"},
    {"id": RAINSPOUT["block"], "pattern": ["SS ", " SC"], "key": {"S": "minecraft:stone_bricks", "C": "minecraft:copper_ingot"},
     "result": RAINSPOUT["block"], "count": 1, "category": "building"},
    {"id": FINIAL["block"], "pattern": ["B", "S", "T"], "key": {"B": "minecraft:bone", "S": "minecraft:stone",
                                                                "T": "minecraft:stone_slab"},
     "result": FINIAL["block"], "count": 1, "category": "redstone"},
]
SHAPED += [{"id": f"{stone}_sarcophagus", "pattern": ["SSS", "BCB", "SSS"], "key": {"S": info["stone"], "B": "minecraft:bone",
                                                                                     "C": "minecraft:chest"},
            "result": f"{stone}_sarcophagus", "count": 1, "category": "building"} for stone, info in STONES.items()]
SHAPELESS = [
    {"id": WARDROBE["block"], "inputs": ["jugcraft:coffin", "minecraft:glass_pane", "minecraft:armor_stand"],
     "result": WARDROBE["block"], "count": 1, "category": "building"},
]
