"""The Mire Hag (docs/features/mire-hag.md; the plan in docs/branches/BOSSES.md, "The Mire Hag: the plan"): the numbers
the Java (src/main/java/.../lair/hag) and the data (tools/mire_hag_data.py) share. tools/check_mod_data.py checks the
Java against them.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).

Her blows (the Clawing Swipe, the roots of her Root Lanes, the hands of her Mire Grasp) are a mob's attack. Her brew's
pool poisons with vanilla's Poison, which never kills (it stops at half a heart) and which milk or the chemical
reactor's Antidote cures.
"""

# MireHagEntity: she, her pacing and her rule.
MIRE_HAG = {
    "HEALTH": 360.0, "ARMOR": 6.0,
    "LEASH": 30.0,               # farther than this from the marsh's middle, she glides back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 40,            # waking: she looks up, the lamps gutter, and she steps down into the water, unhurt
    "WAKE_RADIUS": 18.0,         # she wakes when someone who may fight her comes this near the marsh's middle (the ring)
    "WAKE_NEAR": 6.0,            # or this near her on her porch
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.15,               # wading, as if the water were dry land
    "KEEP_DISTANCE": 2.5,
    "STEP": 1.25,                # how high she steps: out of the water onto a boardwalk (a block and an eighth)
    "DARK_AT": 0.55,             # at this share of her health, Lights Out
    "LAST_AT": 0.2,              # below this share, the Last Light
    "LAST_COOLDOWN": 0.6667,     # her cooldowns and her pause between attacks then: a third shorter
    # Her rule, the light finds her. LIKENESSES of her walk the fen with her (DARK_LIKENESSES after Lights Out), LIKENESS_SPREAD
    # degrees apart round her foe; one that bursts forms again from the mist LIKENESS_RETURN ticks later, out of the light.
    # Within LAMP_REACH (flat) of a burning lamp they gutter and fade, and she glows; lighting a lamp that had gone out
    # shows her for REVEAL_TICKS wherever she is, and bursts every Likeness in its light.
    "LIKENESSES": 2, "DARK_LIKENESSES": 4, "LIKENESS_SPREAD": 50.0, "LIKENESS_RETURN": 200, "LAMP_REACH": 8.0,
    "REVEAL_TICKS": 100,
    # Clawing Swipe: her claws across her front, SWIPE_HALF_ANGLE degrees either side of her face.
    "SWIPE_DAMAGE": 8.0, "SWIPE_REACH": 3.0, "SWIPE_HALF_ANGLE": 60.0,
    # Root Lanes: LANES lanes LANE_LENGTH long from her toward her foe, LANE_SPREAD degrees apart, each LANE_WIDTH either
    # side of its line; whoever the roots burst under takes LANE_DAMAGE and is held fast for ROOT_HOLD ticks.
    "LANES": 3, "LANE_LENGTH": 12.0, "LANE_WIDTH": 0.75, "LANE_SPREAD": 20.0, "LANE_DAMAGE": 6.0, "ROOT_HOLD": 30,
    # Poison Brew: a flask lobbed at her foe shatters into a pool POOL_RADIUS round for POOL_TICKS; every POOL_EVERY ticks
    # whoever stands in it is poisoned for POISON_TICKS (Poison I). At most MAX_POOLS at once.
    "POOL_RADIUS": 1.5, "POOL_TICKS": 120, "POOL_EVERY": 10, "POISON_TICKS": 60, "MAX_POOLS": 6,
    # Lights Out at DARK_AT: she cackles for DARK_TICKS, unhurt, DARK_SNUFFS lamps gutter out, and the Root Knot crawls on
    # a third.
    "DARK_TICKS": 60, "DARK_SNUFFS": 2,
    # Mire Grasp: hands of mud in a ring GRASP_RADIUS round where her foe stood: GRASP_DAMAGE, and Slowness II for
    # GRASP_SLOW ticks.
    "GRASP_RADIUS": 1.5, "GRASP_DAMAGE": 6.0, "GRASP_SLOW": 40, "GRASP_SLOW_AMPLIFIER": 1,
    "EXPERIENCE": 300,
}
# Her attacks (MireHagEntity.Attack): wind-up, active and recovery ticks, cooldown, the range she uses it from (blocks),
# and in which phases ("lure": the first; "drowned": the second, after Lights Out, and the Last Light).
ATTACKS = {
    "CLAWING_SWIPE": (12, 4, 10, 30, 0, 3.5, True, True),
    "ROOT_LANES": (24, 30, 10, 140, 2, 12, True, True),
    "POISON_BREW": (16, 1, 12, 120, 3, 16, True, True),
    "LIKENESS_EXCHANGE": (40, 1, 10, 300, 0, 30, True, True),
    "MIRE_GRASP": (24, 20, 10, 160, 0, 16, False, True),
    "SNUFF": (30, 1, 10, 400, 0, 40, False, True),
}
# Her Likenesses (HagLikenessEntity) never walk into a burning lamp's light of their own accord: a step that would take
# one nearer a lamp's light than SHY blocks turns aside, and it skirts the light.
LIKENESS = {"SHY": 0.5}
# The Root Knot: it bursts from the water on the ring KNOT_START blocks (along the ring) from the lamp it heads for, and
# crawls along the ring toward it at SPEED; within REACH (flat) of the lamp's post, the lamp goes out.
ROOT_KNOT = {"HEALTH": 30.0, "SPEED": 0.06, "START": 10.0, "REACH": 2.0}
# A flask of her brew: it arcs from her hand (HAND_AHEAD blocks ahead of her feet and HAND_UP over them: where her
# model's left hand is as she throws, tools/mire_hag_models.py HAND_Z and HAND_Y) onto its mark, and shatters where it
# lands, or on the first player it strikes.
BREW = {"SPEED": 0.7, "LIFE": 80, "GRAVITY": 0.04, "HAND_AHEAD": 1.375, "HAND_UP": 1.875}
# Her roots and her grasping hands stand for her attack's active ticks, then sink for SINK ticks and are gone.
SINK = 10
# The Hag's Eye: held in the offhand, every EVERY ticks invisible creatures within RANGE glow for GLOW ticks.
EYE = {"RANGE": 16.0, "EVERY": 20, "GLOW": 30}

# Her loot, rolled for each participant (MireHagLoot): Hagmoss, a chance of one of her two trophies (the Arms VII
# variants her bosses/mire_hag table holds; one is certain on a player's first kill), and the chance of each of the
# rest.
HAGMOSS = (3, 6)
TROPHIES = ("hagthorn", "bogfang")
TROPHY_CHANCE = 0.15
CHANCES = {"hags_eye": 0.25, "hags_shawl": 0.2}
ADVANCEMENT = {"key": "seen_through", "title": "Seen Through", "icon": "jugcraft:hagthorn",
               "description": "Find the Mire Hag among her Likenesses and bring her down in the Lantern Fen",
               "frame": "challenge"}

# Her Likenesses go by her name: nothing on them says which is she.
ENTITIES = {
    "mire_hag": "The Mire Hag",
    "hag_likeness": "The Mire Hag",
    "root_knot": "Root Knot",
    "hag_roots": "Hag's Roots",
    "mire_grasp": "Mire Grasp",
    "poison_brew": "Poison Brew",
}
# The entities GeckoLib draws, and the model each is: her Likenesses are she, model and sheet.
GECKO = {"mire_hag": "mire_hag", "hag_likeness": "mire_hag", "root_knot": "root_knot", "hag_roots": "hag_roots",
         "mire_grasp": "mire_grasp", "poison_brew": "poison_brew"}
# The bodies drawn with a glow layer, and so with a glowmask: her eyes and her lantern's flame; the brew's venom.
GLOWING = ("mire_hag", "hag_likeness", "poison_brew")
ITEMS = {"hagmoss": "Hagmoss", "hags_eye": "Hag's Eye"}
COSTUMES = {"hags_shawl": "The Hag's Shawl"}

RECIPES = {
    # Her part of what summons her again: a Hag Stone with two Hagmoss for its fermented spider eye and one of its gold
    # ingots.
    "hag_stone_from_hagmoss": {"shapeless": ["minecraft:mud", "minecraft:mud", "minecraft:string", "minecraft:flint",
                                             "minecraft:gold_ingot", "jugcraft:hagmoss", "jugcraft:hagmoss"],
                               "result": "hag_stone"},
    # For those who never fight her: grey moss for light grey dye. Dye makes no Hagmoss: no loop.
    "light_gray_dye_from_hagmoss": {"shapeless": ["jugcraft:hagmoss"], "result": "minecraft:light_gray_dye", "count": 2},
}

MESSAGES = {
    "wakes": "The Mire Hag looks up from her cauldron: the lamps gutter, and the mist rises",
    "revealed": "The lamplight finds her!",
    "lights_out": "Lights Out: the Hag cackles, and the lamps gutter out",
    "knot": "A knot of roots crawls toward a lamp!",
    "knot_cut": "The Root Knot is cut down, and the lamp burns on",
    "knot_snuffs": "The Root Knot reaches the lamp, and it goes out",
    "last_light": "The Last Light: her lantern flares, and she can hide no longer",
    "reset": "The Mire Hag climbs back onto her porch, and the lamps burn again",
    "defeated": "The Mire Hag falls: her lantern goes out, and the mist lifts",
    "loot": "Her hoard of the fen is yours",
}


def items():
    """Every item the Mire Hag's fight adds (the Hagthorn and the Bogfang are Arms VII variants, listed there)."""
    return list(ITEMS) + list(COSTUMES)
