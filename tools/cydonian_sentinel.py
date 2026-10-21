"""The Cydonian Sentinel (docs/features/cydonian-sentinel.md; the plan in docs/branches/BOSSES.md, "Adam-ondi-Ahman and the
Cydonian Sentinel: the plan", part 3): the numbers the Java (src/main/java/.../lair/sentinel) and the data
(tools/cydonian_sentinel_data.py) share. tools/check_mod_data.py checks the Java against them, and where its fight looks
for the Sanctum's parts against tools/cydonian_sanctum.py.

Units: health and damage in half hearts (vanilla's points), times in ticks (20 a second), distances in blocks. Damage
and health are before the server's lairs.boss_damage and lairs.boss_health scaling (Vesperine's settings, which every
lair boss shares: 0.25 to 4, 1 by default).

Its blows (the Hammerfall, the Quake Line's spikes, the Starfall's meteors) are a mob's attack; its shards (the Shard
Volley, the Storm Shards, the Shard Burst, its sentries' shots) are a mob's projectiles; its lances are magic; the Red
Storm is its own damage type, jugcraft:red_storm.
"""

# CydonianSentinelEntity: it, its pacing and its rule.
SENTINEL = {
    "HEALTH": 480.0, "ARMOR": 12.0,
    "CORE_ARMOR": 4.0,           # the Unbound Core's, with no shell
    "LEASH": 30.0,               # farther than this from the crater's middle, it strides back
    "PARTY_STEP": 0.5, "PARTY_MAX": 2.5,
    "WAKE_TICKS": 50,            # waking: its core flares and it steps down from its plinth, unhurt
    "ABANDON_TICKS": 200, "GLOBAL_COOLDOWN": 20,
    "SPEED": 0.12,               # striding, in its shell
    "CORE_SPEED": 0.2,           # floating, unbound
    "KEEP_DISTANCE": 3.0,
    "STEP": 1.0,
    "CRACK_AT": 0.6,             # at this share of its health, the Shell Cracks
    "UNBOUND_AT": 0.25,          # at this share, the Unbound Core
    "GIANT_AT": 0.1,             # below this share, the Red Giant
    "GIANT_COOLDOWN": 0.6667,    # its cooldowns and its pause between attacks then: a third shorter
    "CRACK_TICKS": 60,           # the Shell Cracks: it roars, unhurt
    "BURST_TICKS": 60,           # the Unbound Core: its shell bursts apart, unhurt
    "CORE_HOVER": 2.0,           # the Unbound Core floats this high over the floor
    # Its rule, the Red Storm: every so often (STORM_EVERY and the rest, below) it plants its arms for STORM_TELL ticks
    # (the sky reddens, dust streams in from one side and the bar names the side), then the storm crosses the crater for
    # STORM_TICKS: everyone not sheltered takes STORM_DAMAGE every STORM_HIT_EVERY ticks and is pushed STORM_PUSH
    # downwind. A player is sheltered when rock (a standing stone, the rim) stands within LEE blocks upwind of them; from
    # anywhere on the crater's floor such a place is within SHELTER_REACH blocks, whichever way the wind blows.
    # Its first storm comes STORM_FIRST ticks after it wakes; then one every STORM_EVERY in the Sentinel, STORM_EVERY_STORM
    # in the Red Storm (and as its shell bursts), STORM_EVERY_UNBOUND unbound and STORM_EVERY_GIANT in the Red Giant.
    "STORM_FIRST": 300, "STORM_EVERY": 700, "STORM_EVERY_STORM": 500, "STORM_EVERY_UNBOUND": 400, "STORM_EVERY_GIANT": 300,
    "STORM_TELL": 50, "STORM_TICKS": 120, "STORM_DAMAGE": 2.0, "STORM_HIT_EVERY": 10, "STORM_PUSH": 0.3,
    "LEE": 8.0, "SHELTER_REACH": 12.0,
    # From the Red Storm on, its Shard Volley rides the wind: STORM_SHARDS shards, STORM_SHARD_AT ticks into the storm,
    # loosed from the windward rim across the crater's width.
    "STORM_SHARDS": 12, "STORM_SHARD_AT": 20,
    # Its Shard Sentries: at most SENTRY_CAP at once; SENTRIES_RISE of them rise at the Shell Cracks and at a Sentry Call.
    "SENTRY_CAP": 4, "SENTRIES_RISE": 2,
    # Hammerfall: both fists slam HAMMER_AHEAD blocks before it: HAMMER_DAMAGE within HAMMER_REACH of where they land, and
    # everyone within HAMMER_KNOCK of it is thrown back HAMMER_THROW.
    "HAMMER_DAMAGE": 14.0, "HAMMER_AHEAD": 2.0, "HAMMER_REACH": 3.0, "HAMMER_KNOCK": 6.0, "HAMMER_THROW": 1.1,
    # Shard Volley: VOLLEY_SHARDS shards in a fan VOLLEY_FAN degrees wide at its foe, each VOLLEY_DAMAGE.
    "VOLLEY_SHARDS": 5, "VOLLEY_FAN": 40.0, "VOLLEY_DAMAGE": 5.0,
    # Quake Line: spikes burst up along a line QUAKE_LENGTH blocks from its feet at its foe: QUAKE_DAMAGE to anyone within
    # QUAKE_WIDTH of it, thrown up QUAKE_TOSS. A spike every QUAKE_STEP blocks down the line.
    "QUAKE_DAMAGE": 10.0, "QUAKE_LENGTH": 14.0, "QUAKE_WIDTH": 1.2, "QUAKE_TOSS": 0.8, "QUAKE_STEP": 2.0,
    # Core Lance: its chest plates open and its core brightens; then a beam LANCE_LENGTH blocks long, LANCE_WIDTH either
    # side of its line, at its foe: LANCE_DAMAGE every LANCE_EVERY ticks. While its plates are open its core takes
    # CORE_MULTIPLIER times the damage and its armour does not count; a blow of STAGGER_DAMAGE or more during the wind-up
    # staggers it for STAGGER_TICKS, and the lance fails.
    "LANCE_DAMAGE": 4.0, "LANCE_EVERY": 5, "LANCE_LENGTH": 16.0, "LANCE_WIDTH": 0.9,
    "CORE_MULTIPLIER": 2.0, "STAGGER_DAMAGE": 6.0, "STAGGER_TICKS": 40,
    # Sweeping Lance: its beam sweeps SWEEP_ANGLE degrees over the attack's active ticks; nothing staggers it.
    "SWEEP_ANGLE": 90.0,
    # Shard Burst: BURST_SHARDS shards fly outward all round it, each BURST_DAMAGE. A standing stone is cover.
    "BURST_SHARDS": 12, "BURST_DAMAGE": 6.0,
    # Starfall: STARFALL_MARKS red marks glow on the ground within MARK_SPREAD blocks of each player (two in the Red Giant);
    # as the wind-up ends meteors strike them: STARFALL_DAMAGE within STARFALL_RADIUS. Each meteor falls from METEOR_HEIGHT
    # over METEOR_FALL ticks, so it lands as the wind-up ends. Then it sinks to the floor, dimmed, for the attack's
    # recovery: the moment for blows.
    "STARFALL_DAMAGE": 10.0, "STARFALL_RADIUS": 2.0, "STARFALL_MARKS": 1, "GIANT_MARKS": 2, "MARK_SPREAD": 3.0,
    "METEOR_HEIGHT": 16.0, "METEOR_FALL": 20,
    "EXPERIENCE": 400,
}
# Its attacks (CydonianSentinelEntity.Attack): wind-up, active and recovery ticks, cooldown, the range it uses it from
# (blocks), and in which phases ("sentinel": the Sentinel; "storm": the Red Storm; "core": the Unbound Core and the Red
# Giant).
ATTACKS = {
    "HAMMERFALL": (16, 4, 12, 60, 0, 4.5, True, True, False),
    "SHARD_VOLLEY": (14, 1, 12, 100, 4, 24, True, True, False),
    "QUAKE_LINE": (20, 10, 12, 140, 0, 14, True, True, False),
    "CORE_LANCE": (24, 30, 14, 220, 3, 16, True, True, False),
    "SENTRY_CALL": (20, 1, 10, 400, 0, 40, False, True, False),
    "SWEEPING_LANCE": (20, 40, 14, 160, 0, 16, False, False, True),
    "SHARD_BURST": (16, 1, 14, 140, 0, 24, False, False, True),
    "STARFALL": (40, 1, 60, 260, 0, 40, False, False, True),
}
# The Red Storm's sides, in the order CydonianSentinelEntity.Wind gives them, and the way each blows (x, z: downwind).
WINDS = {"north": (0, 1), "east": (-1, 0), "south": (0, -1), "west": (1, 0)}
# Where its fight stands in the Sanctum (template cells and points, checked against tools/cydonian_sanctum.py):
# WOKE: where it comes down to the crater's floor as it wakes, before the plinth's steps.
WOKE = (36.5, 28.5)
# CRATER_MIST: the Red Mist it leaves when it falls, in the crater's middle: its columns, its row, three high from the
# floor.
CRATER_MIST = {"x": (35, 37), "z": 46}
# Where its lances leave from: its core, in its shell (CORE_UP blocks over its feet and CORE_AHEAD before them, where
# tools/cydonian_sentinel_models.py puts the core in its chest) and unbound (UNBOUND_UP over its feet, as it floats).
LANCE = {"CORE_UP": 2.5625, "CORE_AHEAD": 0.375, "UNBOUND_UP": 0.75}
# Its Shard Sentries (ShardSentryEntity): RISE ticks rising out of their crater, unhurt; then they float HOVER over the
# floor after the nearest player, keeping KEEP away, and loose a slow shard (SHOT_DAMAGE) every SHOT_EVERY ticks at a
# player within RANGE.
SENTRY = {"HEALTH": 16.0, "SPEED": 0.08, "HOVER": 2.0, "KEEP": 5.0, "RISE": 20, "SHOT_EVERY": 60, "SHOT_DAMAGE": 3.0,
          "RANGE": 20.0}
# A Rust Shard (RustShardEntity): it flies straight at its speed and is gone after LIFE ticks or on anything solid;
# the volley's VOLLEY_SPEED, the storm's STORM_SPEED, the burst's BURST_SPEED, a sentry's SENTRY_SPEED.
SHARD = {"VOLLEY_SPEED": 0.8, "STORM_SPEED": 0.9, "BURST_SPEED": 0.7, "SENTRY_SPEED": 0.35, "LIFE": 80}
# The Quake Line's spikes, and the Starfall's meteors, are what players see, and nothing else: they stand through the
# attack's active ticks, then sink for SINK ticks and are gone.
SINK = 10

# Its loot, rolled for each participant (SentinelLoot): Cydonian Shards, a chance of one of its two trophies (the Arms VII
# variants its bosses/cydonian_sentinel table holds; one is certain on a player's first kill), and the chance of each of
# the rest.
SHARDS = (4, 8)
TROPHIES = ("corelance", "redstar_cleaver")
TROPHY_CHANCE = 0.15
CHANCES = {"lowfall_core": 0.25, "sentinels_visor": 0.2}
ADVANCEMENT = {"key": "red_planet", "title": "Red Planet", "icon": "jugcraft:corelance",
               "description": "Weather the Red Storm and bring down the Cydonian Sentinel on Mars",
               "frame": "challenge"}
# The Lowfall Core: held in the offhand, its holder falls as on Mars: slowly (Slow Falling, refreshed every
# LOWFALL_REFRESH ticks for LOWFALL_TICKS), and so with no fall damage.
LOWFALL_TICKS = 40
LOWFALL_REFRESH = 10

ENTITIES = {
    "cydonian_sentinel": "The Cydonian Sentinel",
    "shard_sentry": "Shard Sentry",
    "rust_shard": "Rust Shard",
    "quake_spike": "Quake Spike",
    "sentinel_meteor": "Meteor",
}
# The entities GeckoLib draws, and the model each is.
GECKO = {entity: entity for entity in ENTITIES}
# The bodies drawn with a glow layer, and so with a glowmask: its core, its visor's slit and the star on its chest; the
# sentries' sparks; the shards' red hearts; the meteors' fire.
GLOWING = ("cydonian_sentinel", "shard_sentry", "rust_shard", "sentinel_meteor")
ITEMS = {"cydonian_shard": "Cydonian Shard", "lowfall_core": "Lowfall Core"}
COSTUMES = {"sentinels_visor": "The Sentinel's Visor"}

RECIPES = {
    # Its part of what summons it again: an Altar Keystone from four shards round a carved stone, with redstone.
    "altar_keystone_from_cydonian_shards": {"shapeless": ["jugcraft:cydonian_shard", "jugcraft:cydonian_shard",
                                                          "jugcraft:cydonian_shard", "jugcraft:cydonian_shard",
                                                          "minecraft:chiseled_stone_bricks", "minecraft:redstone"],
                                            "result": "altar_keystone", "features": ("adam_ondi_ahman",)},
}
# For those who never fight it: the Crusher grinds a shard into rare earth oxide. Nothing makes shards: no loop.
CRUSHING = {"cydonian_shard": {"result": "jugcraft:rare_earth_oxide", "count": 1, "time": 100,
                               "features": ("machines", "rare_earths")}}

# The Red Storm's damage type (data/jugcraft/damage_type/red_storm.json): it scours, unblockable by a shield.
DAMAGE_TYPE = {"message_id": "jugcraft.red_storm", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player",
               "death": "%1$s was scoured away by the Red Storm",
               "death_player": "%1$s was scoured away by the Red Storm while fleeing %2$s"}

MESSAGES = {
    "wakes": "The Cydonian Sentinel's core flares red, and it steps down from its plinth",
    "storm": "The storm comes from the %s",
    "storm_tell": "The Sentinel plants its arms, and the sky reddens: the storm comes from the %s. Shelter in the lee of a standing stone",
    "storm_passes": "The storm passes",
    "cracks": "The Shell Cracks: the Sentinel roars, and its sentries rise from the craters",
    "unbound": "The Unbound Core: its shell bursts apart, and the core floats free",
    "unbound_bar": "The Cydonian Sentinel: the Unbound Core",
    "giant": "Red Giant: the core burns white-hot",
    "giant_bar": "The Cydonian Sentinel: Red Giant",
    "staggered": "Its core is struck: the Sentinel reels, and its lance fails",
    "reset": "The Cydonian Sentinel strides back to its plinth, and its shell reforms",
    "defeated": "The Cydonian Sentinel falls: its core gutters out, and the storm dies",
    "loot": "Its shards are yours",
}
WIND_NAMES = {"north": "north", "east": "east", "south": "south", "west": "west"}


def items():
    """Every item the Cydonian Sentinel's fight adds (the Corelance and the Redstar Cleaver are Arms VII variants, listed
    there)."""
    return list(ITEMS) + list(COSTUMES)
