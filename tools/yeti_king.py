"""The Yeti King (docs/features/yeti-king.md; the plan in docs/branches/BOSSES.md, "The Yeti King: the plan"): the
numbers the Java (src/main/java/.../lair/yeti) and the data (tools/yeti_king_data.py) share. tools/check_mod_data.py
checks the Java against them.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).
"""
FEATURE = "agriculture"

# YetiKingEntity: he, his pacing and his rule.
YETI_KING = {
    "HEALTH": 420.0, "ARMOR": 10.0,
    "LEASH": 30.0,               # farther than this from the lake's centre, he bounds back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 40,            # waking: he roars on his throne and leaps down onto the lake, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.24,               # loping across the snow
    "KEEP_DISTANCE": 2.5,
    "ROAR_AT": 0.5,              # at this share of his health, the King's Roar
    "FURY_AT": 0.2,              # below this share, the Fury of the Peaks
    "FURY_COOLDOWN": 0.6667,     # his cooldowns then: a third shorter
    # Maul Swipe: a fist raked across his front.
    "SWIPE_DAMAGE": 12.0, "SWIPE_REACH": 4.0, "SWIPE_HALF_ANGLE": 60.0, "SWIPE_KNOCKBACK": 1.0,
    # Boulder Throw: the block of ice he hurls, and where it shatters.
    "BOULDER_DAMAGE": 10.0, "BOULDER_RADIUS": 2.5,
    # Ground Slam: he leaps onto his foe, and the snow is blasted bare round where he lands for GLARE_TICKS.
    "SLAM_DAMAGE": 14.0, "SLAM_RADIUS": 3.0, "SLAM_PUSH_RADIUS": 6.0, "SLAM_KNOCKBACK": 1.4,
    "RING": 7.0, "FURY_RING": 10.0, "GLARE_TICKS": 240,
    # Frost Breath: a cone of freezing breath before him.
    "BREATH_REACH": 6.0, "BREATH_HALF_ANGLE": 30.0, "BREATH_DAMAGE": 2.0, "BREATH_EVERY": 5, "BREATH_FROST": 30,
    # Avalanche Charge: a straight run across the lake; into an ice column he stuns himself.
    "CHARGE_SPEED": 0.7, "CHARGE_DAMAGE": 12.0, "CHARGE_KNOCKBACK": 2.0, "STUN_TICKS": 60, "STUN_TAKEN": 1.3333,
    # The King's Roar at half health: back on his dais, unhurt, a blizzard and his kin.
    "ROAR_TICKS": 60, "BLIZZARD_FROST": 120,
    # Icicle Fall: icicles over a marked area round the foe.
    "ICICLES": 8, "ICICLE_AREA": 3.0,
    # Glacial Spikes: spikes of ice bursting up along a line to his foe.
    "SPIKE_LENGTH": 12, "SPIKE_DAMAGE": 10.0, "SPIKE_LIFT": 0.8,
    # Kin Call: whelps out of the dens.
    "KIN": 2, "MAX_WHELPS": 4,
    "EXPERIENCE": 300,
}
# His attacks (YetiKingEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from (blocks),
# and in which phases ("hunt": the first; "blizzard": the second, after the King's Roar; "fury": below a fifth).
ATTACKS = {
    "MAUL_SWIPE": (12, 2, 10, 40, 0, 4.5, True, True, True),
    "BOULDER_THROW": (16, 1, 10, 120, 6, 24, True, True, True),
    "GROUND_SLAM": (16, 16, 14, 200, 4, 16, True, True, True),
    "FROST_BREATH": (20, 30, 10, 160, 0, 6, True, False, False),
    "AVALANCHE_CHARGE": (16, 24, 10, 240, 6, 28, True, True, True),
    "ICICLE_FALL": (20, 16, 10, 160, 0, 30, False, True, True),
    "GLACIAL_SPIKES": (16, 12, 10, 180, 3, 14, False, True, True),
    "KIN_CALL": (20, 1, 10, 300, 0, 40, False, True, True),
}
# The block of ice he hurls: thrown from HELD blocks over his feet, where he holds it over his head.
BOULDER = {"SPEED": 0.9, "LIFE": 80, "GRAVITY": 0.04, "HELD": 4.5}
ICICLE = {"HEIGHT": 12.0, "FALL_SPEED": 0.9, "REACH": 1.0, "DAMAGE": 6.0}
SPIKE = {"RISE_TICKS": 4, "HOLD_TICKS": 20, "WIDTH": 0.8}
WHELP = {"HEALTH": 16.0, "DAMAGE": 3.0, "SPEED": 0.38}
# The Yeti Mitten, held in the offhand: the wearer never freezes.
MITTEN = {"CHECK_TICKS": 1}

# His loot, rolled for each participant (YetiKingLoot): Yeti Fur 4 to 8, a chance of one of his two trophies (the Arms
# VII variants his bosses/yeti_king table holds; one is certain on a player's first kill), and the chance of each of
# the rest.
FUR = (4, 8)
TROPHIES = ("glacier_maul", "rimeclaw")
TROPHY_CHANCE = 0.15
CHANCES = {"yeti_mitten": 0.25, "yeti_king_crown": 0.2}
ADVANCEMENT = {"key": "abominable", "title": "Abominable", "icon": "jugcraft:glacier_maul",
               "description": "Bring down the Yeti King in the Glacier Hall", "frame": "challenge"}

ENTITIES = {
    "yeti_king": "The Yeti King",
    "yeti_whelp": "Yeti Whelp",
    "hurled_boulder": "Hurled Boulder",
    "falling_icicle": "Falling Icicle",
    "glacial_spike": "Glacial Spike",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {name: name for name in ENTITIES}
# The bodies drawn with a glow layer, and so with a glowmask: his eyes and crown, which blaze in the Fury of the Peaks.
GLOWING = ("yeti_king",)
ITEMS = {"yeti_fur": "Yeti Fur", "yeti_mitten": "Yeti Mitten"}
COSTUMES = {"yeti_king_crown": "The Yeti King's Crown"}

RECIPES = {
    # His part of what summons him again: a Frost Horn with Yeti Fur for its two leather and one of its gold ingots.
    "frost_horn_from_fur": {"shapeless": ["minecraft:goat_horn", "minecraft:gold_ingot", "minecraft:snow_block",
                                          "minecraft:snow_block", "jugcraft:yeti_fur"], "result": "frost_horn"},
    # Fur sheared into wool. Less than it is worth in the horn, and wool makes no fur: no loop.
    "white_wool_from_yeti_fur": {"shapeless": ["jugcraft:yeti_fur"], "result": "minecraft:white_wool", "count": 2},
}

MESSAGES = {
    "wakes": "The Yeti King rises from his throne with a roar that shakes the ice",
    "roar": "The King's Roar: a blizzard howls through the hall, and his kin answer",
    "fury": "The Fury of the Peaks: his crown blazes, and his eyes burn blue",
    "stunned": "The Yeti King crashes into the ice and reels",
    "reset": "The Yeti King climbs back onto his throne",
    "defeated": "The Yeti King falls, and the glacier is still",
    "loot": "His frozen hoard is yours",
}


def items():
    """Every item the Yeti King's fight adds (the Glacier Maul and the Rimeclaw are Arms VII variants, listed there)."""
    return list(ITEMS) + list(COSTUMES)
