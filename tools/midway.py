"""The fall fair midway (fall addition 26): the High Striker, Ring Toss and the plush prizes they give. The numbers here
are what agriculture/HighStrikerBlock, Midway, RingTossBlock, TossRing and PlushBlock use; tools/check_mod_data.py
compares them. tools/midway_data.py writes the JSON, tools/midway_textures.py the textures.

The High Striker stands five blocks tall: a strike pad at its foot (part 0), the painted tower with its lamps (parts 1 to
4) and the bell on top. Struck with a Carnival Mallet, its puck climbs `levels` lamps (two a part, on parts 1 to 4) by
the blow's strength: a full swing (a fully charged attack) is `full_low` to `full_high` strong, a critical (falling)
swing `crit_bonus` stronger; `ring_at` or more reaches the top (`levels` + 1) and rings the bell for a prize, less climbs
that share of the lamps. The puck climbs one lamp
every `rise_ticks`, rests `hold_ticks` at its height, then drops back a lamp a tick.

Ring Toss is a crate of nine bottles. A Toss Ring landing on its top within `ringer_radius` pixels of a bottle's neck,
thrown from at least `min_distance` blocks off, is a ringer: it stays on the bottle `ringer_ticks` and wins a prize; any
other landing drops the ring to be thrown again.

A prize is a roll of the loot table jugcraft:gameplay/midway_prize: a plush, weighted by `weight`.
"""
LEVELS = 8
HIGH_STRIKER = {"block": "high_striker", "display": "High Striker", "mallet": "carnival_mallet", "mallet_display": "Carnival Mallet",
                "levels": LEVELS, "full_low": 0.7, "full_high": 1.0, "crit_bonus": 0.15, "ring_at": 0.95, "rise_ticks": 2, "hold_ticks": 30,
                "mallet_damage": 1.0, "mallet_speed": -3.4, "light": 10}
RING_TOSS = {"block": "ring_toss", "display": "Ring Toss", "ring": "toss_ring", "ring_display": "Toss Ring", "ringer_radius": 1.25,
             "min_distance": 3.0, "ringer_ticks": 60, "necks": [3.5, 8.0, 12.5], "top": 13.0, "ring_speed": 0.75}
# Each plush: its name, its words, its weight in the prize table, and its footprint (x0, z0, x1, z1, height) in pixels.
PLUSHES = {
    "pumpkin_plush": {"display": "Pumpkin Plush", "weight": 24, "shape": (3, 3, 13, 13, 8)},
    "ghost_plush": {"display": "Ghost Plush", "weight": 24, "shape": (3, 4, 13, 12, 10)},
    "bat_plush": {"display": "Bat Plush", "weight": 20, "shape": (1, 5, 15, 11, 10)},
    "black_cat_plush": {"display": "Black Cat Plush", "weight": 20, "shape": (4, 3, 12, 12, 12)},
    "squirrel_plush": {"display": "Squirrel Plush", "weight": 9, "shape": (5, 4, 11, 13, 12)},
    "werewolf_plush": {"display": "Werewolf Plush", "weight": 4, "shape": (4, 3, 12, 11, 13)},
    "jumbo_pumpkin_plush": {"display": "Jumbo Pumpkin Plush", "weight": 1, "shape": (1, 1, 15, 15, 14)},
    # The harvest plushes (Halloween decorations batch 16, tools/decor16.py), sculpted on 64 x 64 textures.
    "owl_plush": {"display": "Owl Plush", "weight": 12, "shape": (4, 5, 12, 12, 14), "sculpted": True},
    "hedgehog_plush": {"display": "Hedgehog Plush", "weight": 12, "shape": (4, 2, 12, 13, 7), "sculpted": True},
    "acorn_plush": {"display": "Acorn Plush", "weight": 16, "shape": (4, 4, 12, 12, 13), "sculpted": True},
    "corn_plush": {"display": "Corn Plush", "weight": 16, "shape": (4, 4, 12, 12, 13), "sculpted": True},
    "maple_leaf_plush": {"display": "Maple Leaf Plush", "weight": 16, "shape": (1, 5, 15, 11, 15), "sculpted": True},
}
JACKPOT = "jumbo_pumpkin_plush"
PRIZE_TABLE = "gameplay/midway_prize"

ADVANCEMENTS = {
    "step_right_up": {"icon": "jugcraft:pumpkin_plush", "title": "Step Right Up", "description": "Win a prize at the midway",
                      "frame": "task"},
    "ring_the_bell": {"icon": "jugcraft:high_striker", "title": "Ring the Bell", "description": "Ring the High Striker's bell",
                      "frame": "task"},
    "ringer": {"icon": "jugcraft:toss_ring", "title": "Ringer!", "description": "Land a Toss Ring on a bottle", "frame": "task"},
    "jackpot": {"icon": "jugcraft:jumbo_pumpkin_plush", "title": "Jackpot", "description": "Win the Jumbo Pumpkin Plush",
                "frame": "challenge"},
}

# Recipes: the striker is planks, two redstone lamps and a bell; the mallet a red-banded log head on two sticks; the crate
# six glass bottles over wooden slabs; four rings from two sticks and a string. Plushes are won, not made.
SHAPED = [
    {"id": HIGH_STRIKER["block"], "pattern": ["PBP", "PLP", "PLP"],
     "key": {"P": "#minecraft:planks", "B": "minecraft:bell", "L": "minecraft:redstone_lamp"}, "result": HIGH_STRIKER["block"], "count": 1,
     "category": "building"},
    {"id": HIGH_STRIKER["mallet"], "pattern": ["WLW", " S ", " S "],
     "key": {"W": "minecraft:red_wool", "L": "#minecraft:logs", "S": "minecraft:stick"}, "result": HIGH_STRIKER["mallet"], "count": 1,
     "category": "equipment"},
    {"id": RING_TOSS["block"], "pattern": ["GGG", "GGG", "SSS"],
     "key": {"G": "minecraft:glass_bottle", "S": "#minecraft:wooden_slabs"}, "result": RING_TOSS["block"], "count": 1, "category": "building"},
]
SHAPELESS = [
    {"id": RING_TOSS["ring"], "inputs": ["minecraft:stick", "minecraft:stick", "minecraft:string"], "result": RING_TOSS["ring"], "count": 4,
     "category": "misc"},
]


def blocks():
    return [HIGH_STRIKER["block"], RING_TOSS["block"]] + list(PLUSHES)


def items():
    return blocks() + [HIGH_STRIKER["mallet"], RING_TOSS["ring"]]


def displays():
    out = {HIGH_STRIKER["block"]: HIGH_STRIKER["display"], HIGH_STRIKER["mallet"]: HIGH_STRIKER["mallet_display"],
           RING_TOSS["block"]: RING_TOSS["display"], RING_TOSS["ring"]: RING_TOSS["ring_display"]}
    out.update({name: plush["display"] for name, plush in PLUSHES.items()})
    return out


def neck(index):
    """The centre (x, z) in pixels of bottle `index` (0 to 8, row by row from the north-west)."""
    necks = RING_TOSS["necks"]
    return necks[index % 3], necks[index // 3]
