"""The Pumpkin King (docs/features/pumpkin-king.md; the plan in docs/branches/BOSSES.md, "The Pumpkin King: the plan"):
the numbers the Java (src/main/java/.../lair/king, weapons/FrightBoon.java) and the data (tools/pumpkin_king_data.py)
share. tools/check_mod_data.py checks the Java against them, and where his fight looks for the field's parts against
tools/crowfield.py (his field, part 1: docs/features/crowfield.md).

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks; his
reaches are measured from his middle. Damage and health are before the server's lairs.boss_damage and
lairs.boss_health scaling (Vesperine's settings, which every lair boss shares: 0.25 to 4, 1 by default).
"""

# PumpkinKingEntity: he, his pacing and his rule.
PUMPKIN_KING = {
    "HEALTH": 400.0, "ARMOR": 6.0,
    "LEASH": 26.0,               # farther than this from the field's centre, he is set back in it
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 50,            # waking: he tears free of his post, unhurt, his head bursting into flame
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 16,
    "SPEED": 0.2,                # striding after his foe: slower than a sprinting player
    "KEEP_DISTANCE": 2.5,
    "STEP": 2.1,                 # he strides over the hay stacks (two high), the cart and his mound
    "CROWN_AT": 0.5,             # at this share of his health, the Crowning
    "LAST_AT": 0.25,             # below this share, the Last Harvest
    "CROWNING_TICKS": 40,        # the Crowning: back on his post, his head flaring white, unhurt
    "CROWN_JACKS": 2,            # Jack-o'-Straws that climb out of the rows at the Crowning
    # Straw burns: a blow of fire (burning, a fire weapon, a burning arrow) does FIRE times its damage. Bereft, every blow
    # does BEREFT times, and fire BEREFT_FIRE times.
    "FIRE": 1.5, "BEREFT": 1.5, "BEREFT_FIRE": 2.0,
    # Waking, and after the Crowning, he steps down off his mound onto the arm of the cross (of the four) nearest his foe,
    # DOWN_AT blocks out from the centre.
    "DOWN_AT": 5.0,
    # His rule, the Harvest. Every HARVEST_EVERY ticks (HALLOWED_EVERY in the Hallowed Harvest, LAST_EVERY in the Last
    # Harvest) he calls it (CALL_TICKS, arms raised, the crows crying) on REAPING_GOURDS King's Gourds (HALLOWED_GOURDS
    # from the Crowning on): the one nearest the player he fights and the next round the ring, toward whichever of its
    # neighbours is nearer them. They ripen over RIPEN_TICKS (swelling, then ripening at SWELL_TICKS); any player may pick
    # one meanwhile. One that ripens lights (its face burning) and SPLIT_TICKS later splits: a Jack-o'-Straw climbs out,
    # or, with JACKS_MAX about, its fire feeds him FEED of his health. A picked or split gourd's hill is bare until a green
    # gourd sprouts there REGROW_TICKS later; a bare hill he calls sprouts at once. Every gourd of a harvest picked before
    # any lights leaves him bereft: he sags (STAGGER_TICKS), his fire gutters and his Jacks stand dazed for BEREFT_TICKS.
    "HARVEST_EVERY": 440, "HALLOWED_EVERY": 360, "LAST_EVERY": 300,
    "CALL_TICKS": 30, "RIPEN_TICKS": 200, "SWELL_TICKS": 100, "SPLIT_TICKS": 30, "REGROW_TICKS": 400,
    "REAPING_GOURDS": 2, "HALLOWED_GOURDS": 3,
    "BEREFT_TICKS": 160, "STAGGER_TICKS": 40,
    "JACKS_MAX": 4, "FEED": 0.04,
    # Scythe Sweep: a sweep across his front, SWEEP_REACH blocks, SWEEP_HALF_ANGLE degrees either side of his face.
    "SWEEP_DAMAGE": 8.0, "SWEEP_REACH": 4.5, "SWEEP_HALF_ANGLE": 75.0,
    # Gourd Toss: a flaming pumpkin at a ring of embers where his foe stood (at most TOSS_RANGE away), flying TOSS_FLIGHT
    # ticks: TOSS_DAMAGE within TOSS_SPLASH of where it lands, and set alight TOSS_FIRE_SECONDS.
    "TOSS_DAMAGE": 7.0, "TOSS_SPLASH": 3.0, "TOSS_FIRE_SECONDS": 3, "TOSS_RANGE": 20.0, "TOSS_FLIGHT": 12,
    # Crow Storm: the crows stream along a line from him toward his foe (fixed as he flings his arms wide), STORM_LENGTH
    # long and twice STORM_HALF_WIDTH wide, through its active ticks: STORM_DAMAGE every STORM_EVERY ticks to whoever is
    # in it, and Blindness for STORM_BLIND_TICKS.
    "STORM_DAMAGE": 2.0, "STORM_LENGTH": 14.0, "STORM_HALF_WIDTH": 1.5, "STORM_EVERY": 10, "STORM_BLIND_TICKS": 40,
    # Vine Snare: the vines ripple along the furrow (north and south) toward where his foe stood, and lash up there, along
    # SNARE_HALF_LENGTH of the furrow either way and SNARE_HALF_WIDTH across it: SNARE_DAMAGE, rooted SNARE_ROOT_TICKS
    # (Slowness IV).
    "SNARE_DAMAGE": 5.0, "SNARE_ROOT_TICKS": 30, "SNARE_HALF_WIDTH": 1.0, "SNARE_HALF_LENGTH": 2.5,
    # Head Roll: his burning head rolls along the line toward his foe (fixed as he lifts it), ROLL_LENGTH long and twice
    # ROLL_HALF_WIDTH wide, over its active ticks: ROLL_DAMAGE to each player it meets, thrown aside (ROLL_THROW).
    "ROLL_DAMAGE": 10.0, "ROLL_LENGTH": 18.0, "ROLL_HALF_WIDTH": 1.0, "ROLL_THROW": 0.9,
    "EXPERIENCE": 300,
}
# His attacks (PumpkinKingEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from
# (blocks), and in which phases ("reaping": the Reaping; "hallowed": the Hallowed and the Last Harvest). The wind-up is
# what players read; the strike lands as it ends. The Gourd Toss's active ticks are its pumpkin's flight, the Crow Storm's
# the crows' stream and the Head Roll's the head's roll.
ATTACKS = {
    "SCYTHE_SWEEP": (16, 1, 14, 40, 0, 4.5, True, True),
    "GOURD_TOSS": (20, 12, 10, 100, 5, 20, True, True),
    "CROW_STORM": (24, 40, 12, 160, 0, 14, True, True),
    "VINE_SNARE": (24, 1, 12, 140, 0, 16, False, True),
    "HEAD_ROLL": (30, 24, 14, 200, 4, 18, False, True),
}
# His Jacks (JackOStrawEntity): little straw men with carved gourds for heads, lit inside; a swipe of DAMAGE within REACH
# every SWIPE_COOLDOWN ticks; quick on their feet; fire does FIRE times its damage. They drop nothing, give no experience
# and are never saved.
JACK = {"HEALTH": 20.0, "DAMAGE": 3.0, "SPEED": 0.3, "REACH": 1.5, "SWIPE_COOLDOWN": 20, "FIRE": 2.0}
# Where the Crowning's two Jacks climb out of the rows (x, z in the field's template): on the east-west arm of the cross,
# clear of the hills and the cover.
CROWN_JACK_SPOTS = ((25.5, 32.5), (43.5, 32.5))

# His loot, rolled for each player paid for his fall (PumpkinKingLoot, through lair/LairRewards): the King's Ember, a chance
# of one of his two trophies (the Arms VII variants his bosses/pumpkin_king table holds; one is certain on a player's
# first win), and the chance of each of the rest. During the Halloween event, a roll of seasonal candy and EVENT_CROWN_CHANCE
# more of his crown.
TROPHIES = ("hallowreap", "crowbill")
TROPHY_CHANCE = 0.15
CHANCES = {"crowward_charm": 0.25, "pumpkin_kings_crown": 0.2}
EVENT_CROWN_CHANCE = 0.1
ADVANCEMENT = {"key": "uncrowned", "title": "Uncrowned", "icon": "jugcraft:hallowreap",
               "description": "Leave the Pumpkin King bereft of his harvest, and bring him down in the Crowfield",
               "frame": "challenge"}
# The King's Ember: burns in a furnace as long as a block of coal (EMBER_BURN ticks, 80 items).
EMBER_BURN = 16000
# The Crowward Charm: held in the offhand, crows will not come down on crops within CHARM_REACH blocks of its holder (a lit
# scarecrow's guard, agriculture/Scarecrows.java LIT), out to its HEIGHT above or below.
CHARM = {"CHARM_REACH": 12}

ENTITIES = {
    "pumpkin_king": "The Pumpkin King",
    "jack_o_straw": "Jack-o'-Straw",
    "kings_pumpkin": "The King's Pumpkin",
    "kings_head": "The King's Head",
}
# The entities GeckoLib draws, and the model each is. Each glows somewhere (the King's and his Jacks' carved faces, the
# pumpkin's and the head's fire), so each has a glowmask and a glow layer.
GECKO = {"pumpkin_king": "pumpkin_king", "jack_o_straw": "jack_o_straw", "kings_pumpkin": "kings_pumpkin",
         "kings_head": "kings_head"}
GLOWING = ("pumpkin_king", "jack_o_straw", "kings_pumpkin", "kings_head")
ITEMS = {"kings_ember": "King's Ember", "crowward_charm": "Crowward Charm"}
COSTUMES = {"pumpkin_kings_crown": "The Pumpkin King's Crown"}
BLOCKS = {"kings_lantern": "King's Lantern"}
# The King's Lantern: an heirloom pumpkin carved with his grin round one of his embers, which never goes out (light 15).
LANTERN_LIGHT = 15

RECIPES = {
    # For those who never fight: the ember set in any heirloom pumpkin. Nothing makes an ember but him: no loop.
    "kings_lantern": {"shapeless": ["jugcraft:kings_ember", "#jugcraft:heirloom_pumpkins"], "result": "kings_lantern"},
}

MESSAGES = {
    "wakes": "The Pumpkin King tears free of his post, his head bursts into flame, and the crows burst out of the corn",
    "harvest": "He calls the harvest round %s: pick the ripening gourds before they burn!",
    "harvest_bar": "The Pumpkin King: the harvest ripens",
    "picked": "Gourd picked: %s of %s",
    "jack": "A gourd burns through, and a Jack-o'-Straw climbs out of it!",
    "feeds": "A gourd burns through, and its fire feeds the King",
    "bereft": "Every gourd picked: the Pumpkin King is bereft, and his fire gutters!",
    "bereft_bar": "The Pumpkin King: bereft!",
    "crowning": "The Crowning: he climbs back onto his post, and his head flares white",
    "hallowed_bar": "The Pumpkin King: the Hallowed Harvest",
    "last": "The Last Harvest: he calls the harvest faster",
    "last_bar": "The Pumpkin King: the Last Harvest",
    "reset": "The Pumpkin King hangs himself back on his post, his gourds turn green, and his fire goes dark",
    "defeated": "The Pumpkin King's head drops from his shoulders and rolls to a stop; his fire goes out, and the crows settle",
    "loot": "The King of the harvest is fallen: his spoils are yours",
}


def items():
    """Every item the Pumpkin King's fight adds (the Hallowreap and the Crowbill are Arms VII variants, listed there), and
    the King's Lantern's item."""
    return list(ITEMS) + list(COSTUMES) + list(BLOCKS)


def blocks():
    """The blocks the Pumpkin King's fight adds: the King's Lantern."""
    return list(BLOCKS)
