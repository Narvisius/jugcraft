"""The leaf blower (fall addition 30): a dieselpunk electric leaf blower, charged at the Charging Station like the other
powered tools, that blows fallen leaves into heaps, items and mobs out of the way and candles out, and, sneaking,
vacuums leaves up to compost. The numbers here are what agriculture/LeafBlowerItem uses; tools/check_mod_data.py
compares them. tools/leaf_blower_data.py writes its model (from the powered tools' dieselpunk textures), words and
recipe data.

Held in use it blows: `blow_je` JE a tick, out of `capacity` (a full charge is capacity / blow_je ticks of blowing).
Its stream is a cone `range` blocks long and `cone` degrees either side of where its user looks:
  - items and experience in it are pushed along it (`push_items` blocks a tick, a little less further off), mobs and
    other players more gently (`push_mobs`, less for those that resist knockback); nothing is hurt;
  - every `pile_every` ticks each leaf pile within `pile_range` blocks gives a layer to the block beyond it along the
    stream: onto a pile of its colour there (up to a full pile), or a new pile on open ground; against a wall or a
    different pile it stays, so leaves heap up where the stream stops them;
  - lit candles within `pile_range` blocks blow out.
Sneaking, it vacuums: `vacuum_je` a tick; items within `vacuum_range` blocks in its cone are drawn towards its user, and
every `pile_every` ticks a layer of each leaf pile (and each piece of vanilla leaf litter) within `vacuum_range` comes
up into its user's inventory, for the composter.
"""
LEAF_BLOWER = {
    "item": "leaf_blower", "display": "Leaf Blower", "capacity": 40000, "blow_je": 4, "vacuum_je": 6,
    "range": 8.0, "cone": 25.0, "push_items": 0.12, "push_mobs": 0.05, "pile_range": 6, "pile_every": 4, "vacuum_range": 4,
}

ADVANCEMENTS = {
    "gone_with_the_wind": {"icon": "jugcraft:leaf_blower", "title": "Gone with the Wind",
                           "description": "Blow a pile of leaves along with a Leaf Blower", "frame": "task"},
}

# Steel housing and nozzle, a steel gear for the fan, a basic circuit for its motor's control, iron bars for its
# intake grille and a lever for its trigger. The machines feature makes the steel and the circuit.
SHAPED = [
    {"id": LEAF_BLOWER["item"], "pattern": ["SSS", "GCI", " L "],
     "key": {"S": "#c:plates/steel", "G": "#c:gears/steel", "C": "jugcraft:basic_circuit", "I": "minecraft:iron_bars",
             "L": "minecraft:lever"},
     "result": LEAF_BLOWER["item"], "count": 1, "category": "equipment", "features": ["machines"]},
]


def items():
    return [LEAF_BLOWER["item"]]


def blocks():
    return []
