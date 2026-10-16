"""The Alpha Werewolf (docs/features/alpha-werewolf.md; the plan in docs/branches/BOSSES.md, "The Alpha Werewolf: the
plan"): the numbers the Java (src/main/java/.../lair/alpha) and the data (tools/alpha_werewolf_data.py) share.
tools/check_mod_data.py checks the Java against them, and where his fight looks for the ring's parts against
tools/moonlit_ring.py.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks; his
reaches are measured from his middle. Damage and health are before the server's lairs.boss_damage and
lairs.boss_health scaling (Vesperine's settings, which every lair boss shares: 0.25 to 4, 1 by default).
"""

# AlphaWerewolfEntity: he, his pacing and his rule.
ALPHA_WEREWOLF = {
    "HEALTH": 400.0, "ARMOR": 8.0,
    "LEASH": 26.0,               # farther than this from the clearing's middle, he is set back in it
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 50,            # waking: he rises on the Howling Stone and howls at the moon, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 16,
    "SPEED": 0.22,               # loping after his foe: a little slower than a sprinting player
    "KEEP_DISTANCE": 2.0,
    "STEP": 1.1,                 # he bounds up a block: the logs, the boulders and the stone's steps
    "GATHER_AT": 0.5,            # at this share of his health, the Pack Gathers
    "LAST_AT": 0.25,             # below this share, the Last Hunt
    "GATHER_TICKS": 40,          # the Pack Gathers: he leaps onto the stone and howls long at the moon, unhurt
    # His hide, his kind's: a blow that is not silver's does HIDE of its damage; silver does full. Flushed, his hide
    # fails for FLUSHED_TICKS: every blow does full damage, silver FLUSHED_SILVER times.
    "HIDE": 0.5, "FLUSHED_SILVER": 1.5,
    # His rule, the Hunt. Every HUNT_EVERY ticks (FURY_HUNT_EVERY in the Moon's Fury, LAST_HUNT_EVERY in the Last Hunt)
    # he marks his quarry with a short howl (MARK_TICKS), bounds off to hide in a bell clearing (RUN_TICKS, raking the
    # turf as he goes) and lies in ambush for AMBUSH_TICKS; SHADOW_TICKS before the ambush ends a shadow spreads where
    # the quarry stands, and SPRING_TICKS before it ends he springs from the clearing onto it: SPRING_DAMAGE within
    # SPRING_REACH, the quarry knocked flat (Slowness IV for KNOCKED_TICKS). In the Last Hunt, SWITCH_AT ticks into the
    # ambush he slips through the trees to another clearing (RUN_TICKS). The bell of his clearing rung before he springs
    # flushes him: he staggers out for STAGGER_TICKS, his hide fails for FLUSHED_TICKS, and his pack cowers for
    # COWER_TICKS. Before he springs, a sprinting player anywhere in the clearing can come within reach of any bell once
    # he has gone to ground, and in the Last Hunt one at the stone's foot who sets off as he slips away can come within
    # reach of the bell he slips to (the audit walks both: SPRINT blocks a tick, REACH blocks to use a block). The plan
    # had him slip halfway through the ambush (100 ticks); that left too little time to reach the second bell, so he
    # slips at 60.
    "HUNT_EVERY": 480, "FURY_HUNT_EVERY": 400, "LAST_HUNT_EVERY": 320,
    "MARK_TICKS": 20, "RUN_TICKS": 30, "AMBUSH_TICKS": 200, "SHADOW_TICKS": 24, "SPRING_TICKS": 12,
    "SPRING_DAMAGE": 14.0, "SPRING_REACH": 2.5, "KNOCKED_TICKS": 20,
    "SWITCH_AT": 60,
    "STAGGER_TICKS": 30, "FLUSHED_TICKS": 160, "COWER_TICKS": 80,
    # Flushed, he staggers out of his clearing along its spoke to FLUSHED_TO blocks from the centre, in the open.
    "FLUSHED_TO": 17.0,
    # Waking, and after the Pack Gathers, he leaps down off the stone onto the spoke nearest his foe, DOWN_AT blocks out.
    "DOWN_AT": 6.0,
    # Claw Rake: two rakes across his front RAKE_GAP ticks apart, RAKE_REACH blocks, RAKE_HALF_ANGLE degrees either side
    # of his face: RAKE_DAMAGE each.
    "RAKE_DAMAGE": 6.0, "RAKE_REACH": 3.0, "RAKE_HALF_ANGLE": 70.0, "RAKE_GAP": 6,
    # Pounce: onto the shadow at his foe's feet, at most POUNCE_RANGE away: POUNCE_DAMAGE within POUNCE_REACH, thrown down.
    "POUNCE_DAMAGE": 9.0, "POUNCE_REACH": 2.0, "POUNCE_RANGE": 12.0,
    # Rending Howl: Weakness for WEAKNESS_TICKS on players within HOWL_REACH, and his pack runs faster for PACK_SPEED_TICKS.
    "WEAKNESS_TICKS": 120, "HOWL_REACH": 10.0, "PACK_SPEED_TICKS": 120,
    # Tree Leap: he springs into the trees (TREE_TICKS, unseen, the leaves shivering where he is), then a shadow
    # spreads behind his foe (SHADOW_TICKS) and he drops onto it (the last SPRING_TICKS of it): LEAP_DAMAGE within
    # LEAP_REACH. In the trees he is in the canopy TREE_AT blocks from the centre, TREE_UP over the floor, on the side
    # of his foe; the shadow falls TREE_BEHIND blocks behind them.
    "TREE_TICKS": 30, "LEAP_DAMAGE": 10.0, "LEAP_REACH": 2.5, "TREE_AT": 22.0, "TREE_UP": 11.0, "TREE_BEHIND": 2.0,
    # Hamstring Bite: BITE_DAMAGE to his foe within BITE_REACH, and Slowness II for HAMSTRING_TICKS.
    "BITE_DAMAGE": 8.0, "BITE_REACH": 3.0, "HAMSTRING_TICKS": 40,
    # His pack: PACK_SIZE Packmates, never more.
    "PACK_SIZE": 3,
    "EXPERIENCE": 300,
}
# A player sprinting (blocks a tick), and how far away they can use a block: the audit's measure of the ambush.
SPRINT = 0.2806
REACH = 4.5
# His attacks (AlphaWerewolfEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from
# (blocks), and in which phases ("chase": the Chase; "fury": the Moon's Fury and the Last Hunt). The Pounce's active
# ticks are its leap; the Tree Leap's wind-up is his time in the trees and the shadow behind his foe.
ATTACKS = {
    "CLAW_RAKE": (14, 8, 10, 30, 0, 3.0, True, True),
    "POUNCE": (24, 10, 12, 120, 4, 12.0, True, True),
    "RENDING_HOWL": (20, 1, 14, 240, 0, 10.0, True, True),
    "TREE_LEAP": (54, 1, 14, 200, 0, 30.0, False, True),
    "HAMSTRING_BITE": (18, 1, 12, 80, 0, 3.0, False, True),
}
# His pack (PackmateEntity): young werewolves of his blood, smaller than he is (SCALE of a werewolf's size); no hide,
# silver does SILVER times; a bite of DAMAGE within REACH every BITE_COOLDOWN ticks; fast. Wolfsbane wards them off
# (they circle and growl). They drop nothing and give no experience.
PACKMATE = {"HEALTH": 24.0, "DAMAGE": 4.0, "SPEED": 0.32, "REACH": 1.6, "BITE_COOLDOWN": 20, "SILVER": 1.5,
            "SCALE": 0.85, "COWER_SPEED": 0.2}
# Where his pack lopes out of the trees as he wakes, and as it gathers again: on the path round the clearing, these
# degrees from north (clockwise), away from the camp.
PACK_ANGLES = (-60.0, 0.0, 60.0)
PACK_RADIUS = 20.0
# Raked turf (the lair-only raked_turf): the floor his claws tear as he runs to ground; it remembers which of TURFS it was
# torn from (its "turf" property, in this order) and knits back to that KNIT_TICKS after: long enough to outlast his run
# and his ambush, so the trail can be read.
RAKED = {"KNIT_TICKS": 600}
TURFS = ("minecraft:moss_block", "minecraft:dirt_path", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:rooted_dirt")
# The Packfinder Charm: held in the offhand, every werewolf within CHARM_REACH glows for CHARM_GLOW ticks, renewed every
# CHARM_EVERY.
CHARM = {"CHARM_REACH": 32.0, "CHARM_EVERY": 40, "CHARM_GLOW": 60}

# His loot, rolled for each participant (AlphaWerewolfLoot): the Alpha Pelt, a chance of one of his two trophies (the
# Arms VII variants his bosses/werewolf_alpha table holds; one is certain on a player's first kill), and the chance of
# each of the rest.
TROPHIES = ("moonfang", "howler")
TROPHY_CHANCE = 0.15
CHANCES = {"packfinder_charm": 0.25, "alphas_hood": 0.2}
ADVANCEMENT = {"key": "hunters_moon", "title": "Hunter's Moon", "icon": "jugcraft:moonfang",
               "description": "Flush the Alpha Werewolf from his hiding place and bring him down in the Moonlit Ring",
               "frame": "challenge"}

ENTITIES = {
    "alpha_werewolf": "The Alpha Werewolf",
    "packmate": "Packmate",
}
# The entities GeckoLib draws, and the model each is (the Packmates are drawn as the werewolves are).
GECKO = {"alpha_werewolf": "alpha_werewolf"}
# The bodies drawn with a glow layer, and so with a glowmask: his eyes (shown by the "eyes" controller once he wakes).
GLOWING = ("alpha_werewolf",)
ITEMS = {"alpha_pelt": "Alpha Pelt", "packfinder_charm": "Packfinder Charm"}
COSTUMES = {"alphas_hood": "The Alpha's Hood"}

RECIPES = {
    # For those who never fight: the pelt tanned into four leather (a werewolf's pelt gives two). Leather makes no pelt:
    # no loop. The pelt also counts as a werewolf's pelt for the Howling Totem (#jugcraft:werewolf_pelts), one for one.
    "leather_from_alpha_pelt": {"shapeless": ["jugcraft:alpha_pelt"], "result": "minecraft:leather", "count": 4},
}

MESSAGES = {
    "wakes": "The Alpha Werewolf rises on the Howling Stone and howls at the moon, and his pack lopes out of the trees",
    "hunt_bar": "The pack hunts %s",
    "hunts": "He marks %s for the pack, and bounds off into the trees!",
    "hidden": "He is gone into the dark of the trees: find his trail, and ring the bell of his clearing",
    "slips": "He slips through the trees to another clearing!",
    "flushed": "The bell rings out: the Alpha is flushed from hiding, and his hide fails him!",
    "flushed_bar": "The Alpha Werewolf: flushed!",
    "empty_bell": "The bell rings out over an empty clearing",
    "hide": "His hide turns the blow: silver bites deeper, or flush him with a bell",
    "gathers": "The Pack Gathers: he leaps onto the Howling Stone and howls long at the moon",
    "fury_bar": "The Alpha Werewolf: the Moon's Fury",
    "last_hunt": "The Last Hunt: he will change his route",
    "last_bar": "The Alpha Werewolf: the Last Hunt",
    "reset": "The Alpha Werewolf slinks back to his stone, and his pack melts into the trees",
    "defeated": "The Alpha Werewolf slumps on the turf with a last long howl, and his eyes go dark",
    "loot": "The pack's leader is fallen: his spoils are yours",
}


def items():
    """Every item the Alpha Werewolf's fight adds (Moonfang and the Howler are Arms VII variants, listed there)."""
    return list(ITEMS) + list(COSTUMES)
