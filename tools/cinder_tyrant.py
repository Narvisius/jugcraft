"""The Cinder Tyrant (docs/features/cinder-tyrant.md; the plan in docs/branches/BOSSES.md, "The Cinder Tyrant: the
plan"): the numbers the Java (src/main/java/.../lair/tyrant) and the data (tools/cinder_tyrant_data.py) share.
tools/check_mod_data.py checks the Java against them.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).

His fire (the Kiln Breath, the Lava Wave, the fire patches) is the mod's own flame damage (jugcraft:flame, a fire
damage type), and burning is vanilla's: Fire Resistance stops both. His blows (the Tail Sweep, the Body Slam, the gobs'
and cinders' impact, the Cinderlings' bites) are a mob's attack or projectile, which it never stops.
"""

# CinderTyrantEntity: he, his pacing and his rule.
CINDER_TYRANT = {
    "HEALTH": 440.0, "ARMOR": 10.0,
    "LEASH": 30.0,               # farther than this from the bowl's centre, he bounds back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 40,            # waking: he rises roaring from the crucible and crawls out over its rim, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.18,               # crawling across the floor
    "KEEP_DISTANCE": 3.0,
    "STEP": 1.1,                 # how high he steps: over the crucible's rim and onto a shelf
    "ERUPT_AT": 0.5,             # at this share of his health, the Eruption
    "HEART_AT": 0.2,             # below this share, the Molten Heart
    "HEART_COOLDOWN": 0.6667,    # his cooldowns and his pause between attacks then: a third shorter
    # His rule, the heat is his. Hot, blows on him do HOT_TAKEN of their damage. A Body Slam that lands in a flooded
    # trough (any of its stone under him, or within QUENCH_MARGIN of his feet) quenches him for QUENCH_TICKS: blows do
    # QUENCH_TAKEN, and he crawls at QUENCH_SPEED of his pace. Then his cracks flare back over REHEAT_TICKS.
    "HOT_TAKEN": 0.5, "QUENCH_TICKS": 160, "QUENCH_TAKEN": 1.25, "QUENCH_SPEED": 0.5, "QUENCH_MARGIN": 0.25,
    "REHEAT_TICKS": 20,
    # Tail Sweep: his tail swung round his back and flanks; the front of him, SWEEP_FRONT degrees either side of his
    # head, it never reaches. He coils sideways to his foe as he winds up.
    "SWEEP_DAMAGE": 10.0, "SWEEP_REACH": 4.0, "SWEEP_FRONT": 45.0, "SWEEP_KNOCKBACK": 1.0, "SWEEP_BURN": 3,
    # Ember Spit: gobs of magma arced onto marks round his foe, the first on them, the rest within SPIT_SPREAD.
    "SPIT_GOBS": 3, "HEART_GOBS": 5, "SPIT_SPREAD": 3.0,
    # Body Slam: he leaps onto where his foe stood as he rose.
    "SLAM_DAMAGE": 12.0, "SLAM_RADIUS": 3.0, "SLAM_PUSH_RADIUS": 5.0, "SLAM_KNOCKBACK": 1.4, "SLAM_RISE": 3.0,
    # Kiln Breath: a sector of kiln heat before him, burning every BREATH_EVERY ticks.
    "BREATH_REACH": 8.0, "BREATH_HALF_ANGLE": 30.0, "HEART_BREATH_HALF_ANGLE": 45.0, "BREATH_DAMAGE": 3.0,
    "BREATH_EVERY": 10, "BREATH_BURN": 3,
    # Mantle Shed: shards of his mantle crawl off as Cinderlings.
    "SHED": 2, "MAX_CINDERLINGS": 4,
    # The Eruption at half health: up on the forge's lip, roaring and unhurt, while the channel surges and Cinderlings
    # crawl out of the slag; then down into the bowl.
    "ERUPT_TICKS": 60, "ERUPT_CINDERLINGS": 2, "ERUPT_LEAP": 20,
    # The heat channel's surge: announced, then SURGE_WARN ticks later the slag spills SURGE_SPILL blocks over its banks
    # for SURGE_TICKS. In the Eruption, and every SURGE_EVERY ticks after.
    "SURGE_WARN": 20, "SURGE_TICKS": 80, "SURGE_SPILL": 2, "SURGE_EVERY": 600,
    # In the Eruption one sluice at a time is choked with slag and will not turn; the choke moves every CHOKE_TICKS.
    "CHOKE_TICKS": 400,
    # Cinder Rain: cinders shaken from the vent onto marks round his foe, one every CINDER_EVERY ticks.
    "CINDERS": 6, "CINDER_AREA": 3.0, "CINDER_EVERY": 4,
    # Lava Wave: a low ring of slag rolling out from him to WAVE_REACH over the attack's active ticks. Whoever it passes
    # on the floor burns; whoever is in the air, or a block up (a shelf, the crucible's rim), it passes under.
    "WAVE_REACH": 12.0, "WAVE_DAMAGE": 8.0, "WAVE_BURN": 4,
    # The fire patches the gobs and cinders leave: PATCH_RADIUS round where they land, for PATCH_TICKS; standing in one
    # burns, PATCH_DAMAGE every PATCH_EVERY ticks. At most MAX_PATCHES at once.
    "PATCH_TICKS": 80, "PATCH_RADIUS": 1.0, "PATCH_DAMAGE": 1.0, "PATCH_EVERY": 10, "PATCH_BURN": 2, "MAX_PATCHES": 16,
    "EXPERIENCE": 300,
}
# His attacks (CinderTyrantEntity.Attack): wind-up, active and recovery ticks, cooldown, the range he uses it from
# (blocks), and in which phases ("kiln": the first; "eruption": the second, after the Eruption; "heart": below a fifth).
ATTACKS = {
    "TAIL_SWEEP": (16, 4, 10, 50, 0, 4.5, True, True, True),
    "EMBER_SPIT": (24, 1, 12, 120, 4, 24, True, True, True),
    "BODY_SLAM": (20, 16, 14, 180, 3, 16, True, True, True),
    "KILN_BREATH": (30, 30, 10, 160, 0, 8, True, True, True),
    "MANTLE_SHED": (40, 1, 10, 300, 0, 40, True, True, True),
    "CINDER_RAIN": (24, 24, 10, 160, 0, 30, False, True, True),
    "LAVA_WAVE": (30, 40, 10, 220, 0, 12, False, True, True),
}
# A gob of magma he spits: it arcs from his jaws (MOUTH_AHEAD blocks ahead of his feet and MOUTH_UP over them, where his
# model's jaws meet: tools/cinder_tyrant_models.py MOUTH_Z and MOUTH_Y) onto its mark, and strikes everyone within RADIUS
# of where it lands.
GOB = {"SPEED": 0.8, "LIFE": 80, "GRAVITY": 0.04, "DAMAGE": 8.0, "RADIUS": 1.5, "MOUTH_AHEAD": 2.75, "MOUTH_UP": 0.75}
CINDER = {"HEIGHT": 12.0, "FALL_SPEED": 0.9, "REACH": 1.0, "DAMAGE": 6.0}
CINDERLING = {"HEALTH": 14.0, "DAMAGE": 3.0, "SPEED": 0.34, "BURN_SECONDS": 2}

# His loot, rolled for each participant (CinderTyrantLoot): Tyrant Scales, a chance of one of his two trophies (the
# Arms VII variants his bosses/cinder_tyrant table holds; one is certain on a player's first kill), and the chance of
# each of the rest.
SCALES = (3, 6)
TROPHIES = ("cinderbrand", "magmaw")
TROPHY_CHANCE = 0.15
CHANCES = {"salamander_charm": 0.25, "tyrant_crest": 0.2}
ADVANCEMENT = {"key": "tempered", "title": "Tempered", "icon": "jugcraft:cinderbrand",
               "description": "Bring down the Cinder Tyrant in the Cinder Kiln", "frame": "challenge"}

ENTITIES = {
    "cinder_tyrant": "The Cinder Tyrant",
    "cinderling": "Cinderling",
    "magma_gob": "Magma Gob",
    "falling_cinder": "Falling Cinder",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {name: name for name in ENTITIES}
# The bodies drawn with a glow layer, and so with a glowmask: all of them burn.
GLOWING = tuple(ENTITIES)
ITEMS = {"tyrant_scale": "Tyrant Scale", "salamander_charm": "Salamander Charm"}
COSTUMES = {"tyrant_crest": "The Tyrant's Crest"}

RECIPES = {
    # A scale crushed into magma cream, for Fire Resistance. Magma cream makes no scale: no loop.
    "magma_cream_from_tyrant_scale": {"shapeless": ["jugcraft:tyrant_scale"], "result": "minecraft:magma_cream", "count": 2},
    # His part of what summons him again: a Kiln Seal with two scales for its two blaze powder and one of its gold ingots.
    "kiln_seal_from_scales": {"shapeless": ["minecraft:obsidian", "minecraft:obsidian", "minecraft:obsidian",
                                            "minecraft:obsidian", "minecraft:magma_block", "minecraft:gold_ingot",
                                            "jugcraft:tyrant_scale", "jugcraft:tyrant_scale"], "result": "kiln_seal"},
}

MESSAGES = {
    "wakes": "The Cinder Tyrant rises from the crucible, slag streaming from his plates",
    "quenched": "Steam bursts from the trough: the Tyrant's plates cool black and crack",
    "eruption": "The Eruption: the Tyrant roars from the forge mouth, and the mountain answers",
    "surge": "The heat channel surges!",
    "heart": "The Molten Heart: his cracks blaze white",
    "reset": "The Cinder Tyrant sinks back into the crucible",
    "defeated": "The Cinder Tyrant falls, and the crucible cools to black glass",
    "loot": "His molten hoard is yours",
}


def items():
    """Every item the Cinder Tyrant's fight adds (the Cinderbrand and the Magmaw are Arms VII variants, listed there)."""
    return list(ITEMS) + list(COSTUMES)
