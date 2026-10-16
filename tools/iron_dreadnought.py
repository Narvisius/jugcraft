"""The Iron Dreadnought (docs/features/iron-dreadnought.md; the plan in docs/branches/BOSSES.md, "The Iron Dreadnought:
the plan"): the numbers the Java (src/main/java/.../lair/dreadnought) and the data (tools/iron_dreadnought_data.py) share.
tools/check_mod_data.py checks the Java against them, and where his fight looks for the trench's parts against
tools/foundry_trench.py.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks; his
reaches are measured from his middle. Damage and health are before the server's lairs.boss_damage and
lairs.boss_health scaling (Vesperine's settings, which every lair boss shares: 0.25 to 4, 1 by default).

His blows (the Piston Punch, the Trench Charge, the Blowdown's steam) are a mob's attack; his rivets are a mob's
projectile and his mortar shell an explosion he set off; his Coil Arc, his unrouted discharge and his Sparkers'
discharges are jugcraft:arc, an electric arc (the damage type tag jugcraft:electric), which the Grounding Strap halves.
"""

# IronDreadnoughtEntity: he, his pacing and his rule.
IRON_DREADNOUGHT = {
    "HEALTH": 440.0, "ARMOR": 12.0,
    "LEASH": 34.0,               # farther than this from the pit's middle, he walks back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 50,            # waking: his boilers fire and he steps down out of the gantry, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.14,               # walking
    "KEEP_DISTANCE": 3.5,
    "STEP": 0.6,                 # he steps up nothing a block high: a pylon's footing and the cover stop him
    "TORN_AT": 0.5,              # at this share of his health, the Torn Plate
    "RED_AT": 0.2,               # below this share, Red Line
    "RED_COOLDOWN": 0.6667,      # his cooldowns and his pause between attacks then: a third shorter
    # His rule, the discharge. While his vents are shut a blow does PLATED of its damage. Every DISCHARGE_EVERY ticks
    # (RED_DISCHARGE_EVERY in Red Line) he charges his coils for DISCHARGE_TICKS: the pylon nearest his foe lights (two
    # from the Torn Plate on). A lit breaker thrown before the charge ends routes it: he overloads, his vents open for
    # OVERLOAD_TICKS, staggered for the first OVERLOAD_STAGGER. Left, it runs out down the lit lanes: DISCHARGE_DAMAGE to
    # everyone within DISCHARGE_REACH of one. Either way the breakers spring open SPRING_TICKS after. The charge is long
    # enough for a sprinting player anywhere on the pit's floor to come within reach of the breaker nearest them (the
    # audit walks it: SPRINT blocks a tick, REACH blocks to use a block).
    "PLATED": 0.25,
    "DISCHARGE_EVERY": 400, "RED_DISCHARGE_EVERY": 240, "DISCHARGE_TICKS": 90,
    "OVERLOAD_TICKS": 120, "OVERLOAD_STAGGER": 30,
    "DISCHARGE_DAMAGE": 8.0, "DISCHARGE_REACH": 3.0, "SPRING_TICKS": 20,
    # The Torn Plate at TORN_AT: he halts for TORN_TICKS, unhurt, and fouls the breaker of the pylon nearest his foe; a
    # fouled breaker is knocked loose by CLEAR_USES uses. Whenever his vents shut after that, he fouls another if none is.
    "TORN_TICKS": 40, "CLEAR_USES": 3,
    # Piston Punch: his arm across his front, PUNCH_HALF_ANGLE degrees either side of his face; thrown back hard.
    "PUNCH_DAMAGE": 9.0, "PUNCH_REACH": 4.5, "PUNCH_HALF_ANGLE": 60.0, "PUNCH_THROW": 1.6,
    # Rivet Fan: RIVETS hot rivets in a fan at his foe, FAN_STEP degrees apart: RIVET_DAMAGE each.
    "RIVETS": 5, "FAN_STEP": 10.0, "RIVET_DAMAGE": 4.0,
    # Trench Charge: straight ahead at CHARGE_SPEED for at most CHARGE_RANGE blocks: CHARGE_DAMAGE to everyone he runs
    # through (once each), thrown aside. Into a block (cover, a footing, the wall), he is stunned for STUN_TICKS with a
    # vent knocked open.
    "CHARGE_DAMAGE": 10.0, "CHARGE_SPEED": 0.8, "CHARGE_RANGE": 24.0, "CHARGE_THROW": 1.4, "STUN_TICKS": 40,
    # Coil Arc: ARC_DAMAGE to his foe, then on to the nearest other player within ARC_JUMP of the last, ARC_JUMPS times.
    "ARC_DAMAGE": 5.0, "ARC_JUMPS": 3, "ARC_JUMP": 4.0,
    # Mortar: the mark is laid at his foe's feet as he kneels; the shell is fired at the end of the wind-up and lands on
    # the mark SHELL_FLIGHT ticks later: MORTAR_DAMAGE within MORTAR_REACH.
    "MORTAR_DAMAGE": 10.0, "MORTAR_REACH": 3.0, "SHELL_FLIGHT": 20,
    # Blowdown: BLOWDOWN_DAMAGE within BLOWDOWN_REACH of his middle, and Slowness II for SLOW_TICKS.
    "BLOWDOWN_DAMAGE": 6.0, "BLOWDOWN_REACH": 5.0, "SLOW_TICKS": 40,
    # His Sparkers: at most SPARKER_CAP at once; DROPPED of them drop at a time.
    "SPARKER_CAP": 4, "DROPPED": 2,
    "EXPERIENCE": 300,
}
# A player sprinting (blocks a tick), and how far away they can use a block: the audit's measure of the charge.
SPRINT = 0.2806
REACH = 4.5
# His attacks (IronDreadnoughtEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from
# (blocks), and in which phases ("advance": the Advance; "overcharge": the Overcharge and Red Line). The Trench Charge's
# active ticks are the most he runs (CHARGE_RANGE at CHARGE_SPEED).
ATTACKS = {
    "PISTON_PUNCH": (16, 3, 12, 40, 0, 4.5, True, True),
    "RIVET_FAN": (18, 1, 12, 100, 4, 24, True, True),
    "TRENCH_CHARGE": (24, 30, 16, 200, 8, 24, True, True),
    "COIL_ARC": (20, 1, 10, 120, 0, 16, True, True),
    "MORTAR": (30, 1, 14, 160, 6, 32, False, True),
    "BLOWDOWN": (20, 4, 12, 140, 0, 5.0, False, True),
    "SPARKERS": (24, 1, 10, 300, 0, 40, False, True),
}
# Where his fight stands in the trench (template cells and points, checked against tools/foundry_trench.py):
# WOKE: where he comes down to as he wakes, just out of the gantry's mouth on the pit's floor.
WOKE = (35.5, 18.5)
# Who wakes him by stepping into the pit: a player over one of its open cells standing lower than STAND + INTRUDER_UP
# (on its floor: the ramp's lowest step, the footings and the catwalks are higher).
INTRUDER_UP = 0.5
# His Sparkers (SparkerEntity): little coil crawlers that scuttle at the nearest player; within REACH they crackle for
# FUSE ticks and discharge (DAMAGE within REACH) and are spent; struck first, they die. Gone after LIFE ticks.
SPARKER = {"HEALTH": 6.0, "DAMAGE": 4.0, "SPEED": 0.3, "ARMOR": 0.0, "REACH": 1.5, "FUSE": 12, "LIFE": 600}
# A hot rivet (HotRivetEntity): it leaves his battery on his left pauldron (BATTERY_AHEAD blocks ahead of his feet,
# BATTERY_SIDE to his left and BATTERY_UP over them: tools/iron_dreadnought_models.py BATTERY_Z, BATTERY_X and
# BATTERY_Y), flies straight at SPEED, and is gone after LIFE ticks or on striking anything solid.
RIVET = {"SPEED": 1.1, "LIFE": 30, "BATTERY_AHEAD": 1.25, "BATTERY_SIDE": 2.0625, "BATTERY_UP": 4.375}
# A mortar shell (MortarShellEntity): from the mortar raised out of the hatch on his back (MORTAR_BACK behind his feet
# and MORTAR_UP over them: tools/iron_dreadnought_models.py HATCH_Y), up over an arc SHELL_PEAK higher than its ends,
# down onto the mark.
SHELL = {"MORTAR_UP": 5.0625, "MORTAR_BACK": 0.5, "SHELL_PEAK": 8.0}
# The tops of his coils (COIL_SIDE either side of his middle, COIL_UP over his feet: tools/iron_dreadnought_models.py
# COIL_TOP), where his arcs and his discharge leave him.
COILS = {"COIL_SIDE": 2.0625, "COIL_UP": 6.0}
# The Grounding Strap: held in the offhand, harm of the damage type tag jugcraft:electric does STRAP_SHARE of itself.
STRAP_SHARE = 0.5
# The damage type of his arcs, and the tag of the electric harm the Strap halves (lightning, a live power line's
# electrocution, a Tesla gun's zap, and his arcs).
ARC = "arc"
ELECTRIC = ("minecraft:lightning_bolt", "jugcraft:electrocution", "jugcraft:zap", "jugcraft:arc")

# His loot, rolled for each participant (IronDreadnoughtLoot): Salvaged Coil, a chance of one of his two trophies (the
# Arms VII variants his bosses/iron_dreadnought table holds; one is certain on a player's first kill), and the chance of
# each of the rest.
SALVAGED_COIL = (3, 6)
TROPHIES = ("dynamo_halberd", "piston_hammer")
TROPHY_CHANCE = 0.15
CHANCES = {"grounding_strap": 0.25, "dreadnought_visor": 0.2}
ADVANCEMENT = {"key": "grounded", "title": "Grounded", "icon": "jugcraft:dynamo_halberd",
               "description": "Route the Iron Dreadnought's discharge into the ground and bring him down in the Foundry Trench",
               "frame": "challenge"}

ENTITIES = {
    "iron_dreadnought": "The Iron Dreadnought",
    "sparker": "Sparker",
    "hot_rivet": "Hot Rivet",
    "mortar_shell": "Mortar Shell",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {"iron_dreadnought": "iron_dreadnought", "sparker": "sparker", "hot_rivet": "hot_rivet",
         "mortar_shell": "mortar_shell"}
# The bodies drawn with a glow layer, and so with a glowmask: his searchlight eye, his coils, his gauges and the
# furnaces behind his vents (hidden by their shutters while they are shut); the Sparker's coil; the rivet's heat; the
# shell's fuse.
GLOWING = ("iron_dreadnought", "sparker", "hot_rivet", "mortar_shell")
ITEMS = {"salvaged_coil": "Salvaged Coil", "grounding_strap": "Grounding Strap"}
COSTUMES = {"dreadnought_visor": "The Dreadnought Visor"}

RECIPES = {
    # His part of what calls him up again: Sealed Orders with two Salvaged Coil for the steel plate and the wire.
    "sealed_orders_from_salvaged_coil": {"shapeless": ["minecraft:paper", "minecraft:paper", "minecraft:paper",
                                                       "jugcraft:salvaged_coil", "jugcraft:salvaged_coil",
                                                       "minecraft:honeycomb"],
                                         "result": "sealed_orders"},
    # For those who never fight him: the charge left in a coil, scraped out as redstone. Not copper wire: the audit
    # counts the coil as no metal, so wire from it would be metal made from nothing. Redstone makes no Salvaged Coil: no
    # loop.
    "redstone_from_salvaged_coil": {"shapeless": ["jugcraft:salvaged_coil"], "result": "minecraft:redstone", "count": 2},
}

MESSAGES = {
    "wakes": "The Iron Dreadnought's boilers fire: his searchlight burns, and he steps down from the gantry",
    "discharge_bar": "The Iron Dreadnought: discharge to %s!",
    "and": "%s and %s",
    "overload_bar": "The Iron Dreadnought: overloaded!",
    "red_bar": "The Iron Dreadnought: Red Line",
    "plated": "His plating turns the blow: route his discharge into a pylon",
    "charging": "His coils whine: the discharge will strike %s. Throw its breaker!",
    "routed": "The breaker slams home: the bolt is earthed, and the surge throws the Dreadnought's vents open!",
    "unrouted": "The discharge runs out down the lane",
    "fouled": "A plate of his hull jams the breaker: knock it loose (%s more)",
    "cleared": "The plate clangs loose: the breaker is clear",
    "torn": "The Torn Plate: he tears a plate from his hull and hurls it into a breaker",
    "stunned": "He crashes to a stop and staggers, a vent knocked open!",
    "red_line": "Red Line: his boilers redline, and his vents jam open",
    "reset": "The Iron Dreadnought walks back to his gantry, and the pylons go dark",
    "defeated": "The Iron Dreadnought sags in his tracks with a long hiss of steam, and his searchlight goes dark",
    "loot": "His salvage is yours",
}


def items():
    """Every item the Iron Dreadnought's fight adds (the Dynamo Halberd and the Piston Hammer are Arms VII variants,
    listed there)."""
    return list(ITEMS) + list(COSTUMES)
