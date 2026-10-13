"""Vesperine, the Last Reaper (docs/features/vesperine.md; the plan in docs/features/witching-season.md, "Boss 1"): the
numbers the Java (src/main/java/.../lair/vesperine, weapons/HarvestBoon.java) and the data (tools/vesperine_data.py)
share. tools/check_mod_data.py checks the Java against them.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (0.25 to 4; 1 by default).
"""
FEATURE = "agriculture"

# Her settings in config/jugcraft.properties (JugcraftConfig.TEXT_OPTIONS), with their defaults and ranges.
OPTIONS = {"lairs.boss_health": "1.0", "lairs.boss_damage": "1.0", "lairs.event_loot": "on"}
LIMITS = {"lairs.boss_health": (0.25, 4.0), "lairs.boss_damage": (0.25, 4.0)}

# VesperineEntity: she, her guard and her pacing.
VESPERINE = {
    "HEALTH": 400.0, "ARMOR": 10.0, "LEASH": 32.0,
    "GUARD": 0.5,            # what she takes of a blow while both skulls live
    "UNARMED": 1.25,         # ... while her scythe is thrown
    "PARTY_STEP": 0.5,       # her health grows by half for each player after the first ...
    "PARTY_MAX": 2.5,        # ... up to two and a half times
    "RISE_TICKS": 40, "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "GLIDE_SPEED": 0.22, "HOVER": 0.25, "KEEP_DISTANCE": 2.5,
    "ARC_DAMAGE": 14.0, "ARC_REACH": 4.5, "ARC_HALF_ANGLE": 135.0, "ARC_WITHER_TICKS": 60,
    "LUNGE_DAMAGE": 10.0, "LUNGE_DISTANCE": 8.0, "LUNGE_SPEED": 1.0, "LUNGE_WIDTH": 1.5,
    "THROW_RANGE": 16.0, "REARM_TICKS": 160,
    "CALL_THRALLS": 3, "MAX_THRALLS": 4,
    "TOLL_TICKS": 60, "TOLL_DARKNESS": 60, "TOLL_RISE": 6.0,
    "BEAM_DAMAGE": 4.0, "BEAM_INTERVAL": 5, "BEAM_SWEEP": 30, "BEAM_LENGTH": 16.0, "BEAM_WIDTH": 1.0,
    "CIRCLES": 3, "CIRCLE_RADIUS": 2.0, "CIRCLE_DAMAGE": 12.0,
    "STEP_BEHIND": 2.0,
    "HARVEST_AT": 0.25, "HARVEST_TICKS": 120, "HARVEST_DARKNESS": 120, "HARVEST_RISE": 10.0, "SOUL_INTERVAL": 6,
    "SOUL_HEAL": 0.02, "WARD_REACH": 3.0, "SLAM_DAMAGE": 16.0, "SLAM_RADIUS": 10.0,
    "EXPERIENCE": 300,
}
# Her attacks (VesperineEntity.Attack): wind-up, active and recovery ticks, cooldown, the range she uses it from
# (blocks), and in which phases ("reaping": the first, "moon": the second).
ATTACKS = {
    "REAPING_ARC": (12, 1, 10, 40, 0, 4, True, True),
    "HARVEST_LUNGE": (10, 8, 10, 80, 4, 12, True, True),
    "SCYTHE_THROW": (14, 1, 10, 160, 5, 16, True, True),
    "GRAVE_CALL": (30, 1, 10, 300, 0, 40, True, False),
    "TWIN_BEAM": (30, 40, 10, 200, 0, 24, False, True),
    "CROP_CIRCLES": (30, 1, 10, 160, 0, 40, False, True),
    "SHADOW_STEP": (10, 1, 0, 140, 0, 40, False, True),
}
SKULL = {"HEALTH": 80.0, "BOLT_INTERVAL": 80, "JAW_TICKS": 10, "FOLLOW_SPEED": 0.35}
BOLT = {"DAMAGE": 6.0, "SLOW_TICKS": 60, "REFLECTED_DAMAGE": 12.0, "SPEED": 0.35, "REFLECTED_SPEED": 0.6, "TURN": 0.15,
        "LIFE": 160}
SCYTHE = {"DAMAGE": 10.0, "SPEED": 0.9, "REACH": 1.5, "LIFE": 200}
THRALL = {"HEALTH": 20.0, "DAMAGE": 4.0, "EMERGE_TICKS": 20}
SOUL = {"SPEED": 0.3, "LIFE": 240}
# The Vesper Scythe's Harvest boon (weapons/HarvestBoon.java).
HARVEST = {"HEAL": 4.0, "HEAL_COOLDOWN": 100, "CRESCENT_KILLS": 5, "CRESCENT_RANGE": 12.0, "CRESCENT_DAMAGE": 8.0}
CRESCENT = {"SPEED": 1.0}

# Her loot, rolled for each participant (VesperineLoot): Reaper's Shade 3 to 6, and the chance of each of the rest.
SHADE = (3, 6)
CHANCES = {"vesper_scythe": 0.15, "dirge_skull": 0.5, "requiem_skull": 0.5, "reaper_hood": 0.2}
EVENT_HOOD_CHANCE = 0.1   # one more chance at the hood during the Halloween event
ADVANCEMENT = {"key": "the_last_harvest", "title": "The Last Harvest", "icon": "jugcraft:vesper_scythe",
               "description": "Bring down Vesperine, the Last Reaper, in the Hollow Acre", "frame": "challenge"}

ENTITIES = {
    "vesperine": "Vesperine, the Last Reaper",
    "dirge": "Dirge",
    "requiem": "Requiem",
    "grief_bolt": "Grief Bolt",
    "thrown_scythe": "Thrown Scythe",
    "grave_thrall": "Grave Thrall",
    "harvest_soul": "Soul of the Harvest",
    "reaping_crescent": "Reaping Crescent",
}
# The entities GeckoLib draws, and the model each is (Dirge and Requiem share the skull's).
GECKO = {"vesperine": "vesperine", "dirge": "reaper_skull", "requiem": "reaper_skull", "thrown_scythe": "thrown_scythe",
         "grave_thrall": "grave_thrall"}
ITEMS = {"reaper_shade": "Reaper's Shade", "reaper_hood": "Reaper's Hood"}
TROPHIES = {"dirge_skull": "Dirge Skull", "requiem_skull": "Requiem Skull"}

RECIPES = {
    # The Shade Wreath: a cheaper Mourning Wreath, two mourning flowers and a Reaper's Shade (the summons' link).
    "mourning_wreath_from_shade": {"shapeless": ["#jugcraft:mourning_flowers", "#jugcraft:mourning_flowers",
                                                 "jugcraft:reaper_shade"], "result": "mourning_wreath"},
    # The Reaper's Hood, from five shades.
    "reaper_hood": {"pattern": ["SSS", "S S"], "key": {"S": "jugcraft:reaper_shade"}, "result": "reaper_hood"},
}

MESSAGES = {
    "rises": "Vesperine rises from the Bone Throne",
    "toll": "The Last Toll sounds: the moon turns red",
    "harvest": "Death's Harvest: the souls stream to her. Strike them down, or light the wards!",
    "reset": "Vesperine returns to her throne",
    "defeated": "Vesperine, the Last Reaper, has fallen",
    "loot": "The Last Reaper's harvest is yours",
}


def items():
    """Every item Vesperine's fight adds (the Vesper Scythe is an Arms VII variant, listed there)."""
    return list(ITEMS) + list(TROPHIES)


def blocks():
    return list(TROPHIES)
