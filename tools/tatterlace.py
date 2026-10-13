"""Madame Tatterlace (docs/features/tatterlace.md; the plan in docs/features/witching-season.md, "Boss 2"): the numbers
the Java (src/main/java/.../lair/tatterlace, weapons/StitchBoon.java) and the data (tools/tatterlace_data.py) share.
tools/check_mod_data.py checks the Java against them.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).
"""
FEATURE = "agriculture"

# TatterlaceEntity: she, her pacing and her rule.
TATTERLACE = {
    "HEALTH": 360.0, "ARMOR": 8.0,
    "LEASH": 30.0,               # farther than this from the doily's centre, she climbs back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "DESCEND_TICKS": 40,         # waking: she lowers herself from the threads onto the doily
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.26,               # scuttling across the lace
    # Heights over the doily's lace layer (tools/spindle_loft.py LACE): where she waits, sewing, on the white silk over the
    # doily's centre (SPOOL_TOP + 4, the top of its thread), and where her feet hang below it in phase 2, her dragline
    # (tools/tatterlace_models.py) reaching up to the thread's middle.
    "PERCH": 14.5625,
    "HANG": 9.0,
    "HANG_SPEED": 0.2,
    "KEEP_DISTANCE": 2.0,
    "FRENZY_AT": 0.2,            # below this share of her health, Frenzied Stitching
    "FRENZY_COOLDOWN": 0.6667,   # her cooldowns then: a third shorter
    # Needlepoint: two quick stabs of her forelegs, in front of her.
    "STAB_DAMAGE": 8.0, "STAB_REACH": 3.5, "STAB_HALF_ANGLE": 40.0, "STAB_GAP": 5,
    # Spool Roll: the spool she kicks.
    "SPOOL_DAMAGE": 10.0, "SPOOL_KNOCKBACK": 1.6,
    # Taking In the Seams: she climbs into the threads, the light dims, egg sacs ring the doily.
    "TAKE_IN_TICKS": 60, "EGG_SACS": 6,
    # Pin Rain: pins over a marked area round the foe.
    "PINS": 12, "PIN_DAMAGE": 4.0, "PIN_AREA": 2.5,
    # Unravel: a segment of lace frays, drops away, and is knitted back.
    "FRAY_TICKS": 40, "KNIT_TICKS": 240, "SEGMENT_WIDTH": 3.0, "WEDGE_DEGREES": 60.0,
    # Drop Strike: from the threads onto a foe, then open to attack on the lace.
    "DROP_DAMAGE": 14.0, "DROP_RADIUS": 3.0, "DROP_OPEN_TICKS": 60,
    # Brood: spiderlings out of the egg sacs.
    "BROOD": 4, "MAX_SPIDERLINGS": 6,
    "EXPERIENCE": 300,
}
# Her attacks (TatterlaceEntity.Attack): wind-up, active and recovery ticks, cooldown, the range she uses it from
# (blocks), and in which phases ("fitting": the first, "final": the second, hanging in the threads; "frenzy": the third,
# on the lace again).
ATTACKS = {
    "NEEDLEPOINT": (10, 6, 10, 40, 0, 3.5, True, False, True),
    "THIMBLE_TOSS": (10, 1, 10, 100, 4, 16, True, False, True),
    "BINDING_THREAD": (12, 1, 10, 160, 3, 16, True, False, True),
    "LACE_SNARE": (10, 1, 10, 140, 3, 14, True, False, True),
    "SPOOL_ROLL": (16, 1, 12, 240, 0, 40, True, False, True),
    "PIN_RAIN": (20, 10, 10, 100, 0, 40, False, True, True),
    "UNRAVEL": (40, 1, 10, 200, 0, 40, False, True, True),
    "DROP_STRIKE": (16, 1, 60, 160, 0, 40, False, True, False),
    "BROOD": (20, 1, 10, 300, 0, 40, False, True, True),
}
THIMBLE = {"DAMAGE": 6.0, "BOUNCES": 2, "SPEED": 0.7, "LIFE": 120}
THREAD = {"SPEED": 1.2, "LIFE": 30, "TETHER_TICKS": 60, "TETHER_REACH": 4.0, "REEL": 0.05, "HEALTH": 1.0}
SNARE = {"TICKS": 120, "SIZE": 3, "SLOWNESS": 3, "SPEED": 0.6}
SPOOL = {"SPEED": 0.5, "LIFE": 100, "WIDTH": 1.5}
SPIDERLING = {"HEALTH": 6.0, "DAMAGE": 2.0, "POISON_TICKS": 40, "SPEED": 0.35}
EGG_SAC = {"HEALTH": 12.0, "HATCH_TICKS": 20}
# Where Taking In the Seams spits her egg sacs: round the doily at this radius from its centre, at these angles (degrees
# from east, towards south), clear of the spools' barrels, the thimble (east) and the tape's foot (south).
SAC_RADIUS = 17.5
SAC_ANGLES = (25, 70, 155, 205, 290, 335)
# The Needle Rapier's Stitch boon (weapons/StitchBoon.java): three hits on one foe within the window stitch it.
STITCH = {"HITS": 3, "WINDOW": 80, "SLOW_TICKS": 40, "SLOW_AMPLIFIER": 1}
# The Golden Thimble, held in the offhand: it turns aside the first projectile every so often.
GOLDEN_THIMBLE = {"COOLDOWN": 300}

# Her loot, rolled for each participant (TatterlaceLoot): Gossamer Silk 4 to 8, and the chance of each of the rest.
SILK = (4, 8)
CHANCES = {"needle_rapier": 0.15, "golden_thimble": 0.25, "tatterlace_headdress": 0.2}
EVENT_HEADDRESS_CHANCE = 0.1   # one more chance at the headdress during the Halloween event
ADVANCEMENT = {"key": "unravelled", "title": "Unravelled", "icon": "jugcraft:needle_rapier",
               "description": "Bring down Madame Tatterlace in the Spindle Loft", "frame": "challenge"}

ENTITIES = {
    "tatterlace": "Madame Tatterlace",
    "tossed_thimble": "Tossed Thimble",
    "binding_thread": "Binding Thread",
    "lace_snare": "Lace Snare",
    "rolling_spool": "Rolling Spool",
    "tatter_egg_sac": "Egg Sac",
    "tatter_spiderling": "Spiderling",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {"tatterlace": "tatterlace", "tossed_thimble": "tossed_thimble", "rolling_spool": "rolling_spool",
         "tatter_egg_sac": "tatter_egg_sac", "tatter_spiderling": "tatter_spiderling"}
# The bodies drawn with a glow layer, and so with a glowmask: her eyes, gem and red cuffs, and her spiderlings' eyes. The
# thimble, the spool and the egg sac have nothing that glows, so no glowmask and no layer.
GLOWING = ("tatterlace", "tatter_spiderling")
ITEMS = {"gossamer_silk": "Gossamer Silk", "golden_thimble": "Golden Thimble"}
COSTUMES = {"tatterlace_headdress": "Tatterlace's Headdress"}

RECIPES = {
    # Her part of what summons her again: a Cursed Spindle with Gossamer Silk for two of its string and two gold nuggets
    # for one of its gold ingots.
    "cursed_spindle_from_silk": {"shapeless": ["jugcraft:gossamer_silk", "minecraft:gold_ingot", "minecraft:gold_nugget",
                                               "minecraft:gold_nugget", "minecraft:amethyst_shard", "minecraft:spider_eye",
                                               "minecraft:spider_eye", "minecraft:string", "minecraft:stick"],
                                 "result": "cursed_spindle"},
    # Silk unpicked into string, for the Spider's Larder and anything else string makes. Less than it is worth in the
    # spindle, and string makes no silk: no loop.
    "string_from_gossamer_silk": {"shapeless": ["jugcraft:gossamer_silk"], "result": "minecraft:string", "count": 3},
}

MESSAGES = {
    "wakes": "Madame Tatterlace lowers herself onto the doily: \"Hold still, dear, while I take your measurements.\"",
    "take_in": "Taking In the Seams: she climbs into the threads, and the light dims",
    "frenzy": "Frenzied Stitching: her cuffs glow red",
    "reset": "Madame Tatterlace goes back to her sewing",
    "defeated": "Madame Tatterlace has come apart at the seams",
    "loot": "Her sewing basket is yours",
}


def items():
    """Every item Tatterlace's fight adds (the Needle Rapier is an Arms VII variant, listed there)."""
    return list(ITEMS) + list(COSTUMES)
