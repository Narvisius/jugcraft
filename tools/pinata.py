"""The piñata party (fall addition 28): papier-mâché piñatas hung from a ceiling or a branch, filled by the party, swung
at blindfolded until they burst. The numbers here are what agriculture/Pinata, PinataItem and Pinatas use;
tools/check_mod_data.py compares them. tools/pinata_data.py writes the JSON and the quads the client draws them from,
and tools/pinata_textures.py the textures.

A piñata is hung from the underside of any block with a top or bottom to hang from (a ceiling, a beam, a branch, a
fence), its body `drop` blocks below on a rope. Anyone can fill it: using it with an item puts the whole stack in, up
to `slots` stacks. Its hanger can take it down again (sneaking, with an empty hand): its contents come back out and the
piñata comes back as an item. A charged swing (at least `charged` of the hitter's attack damage) is a hit; a swing with
the Piñata Stick counts `stick_hits`. Each piñata takes `hits` hits; on the last it bursts in confetti and its contents
spray out. Weak swings only rock it. Nothing else hurts it. If what it hangs from goes, it falls and drops itself and its
contents.

The Blindfold is worn on the head and blacks out the wearer's view but for a sliver at the bottom (an equippable's
camera overlay, as a carved pumpkin's is). Bursting a piñata earns Piñata Party; bursting one blindfolded, Blind Luck.
"""
PINATA = {
    "entity": "pinata", "entity_display": "Piñata", "slots": 9, "drop": 1.5, "width": 0.9, "height": 0.9,
    "charged": 0.75, "stick": "pinata_stick", "stick_display": "Piñata Stick", "stick_hits": 2,
    "blindfold": "blindfold", "blindfold_display": "Blindfold",
}
# The kinds: item, words, the hits each takes, and its paper's colours (for the confetti, as RGB).
KINDS = {
    "pumpkin": {"item": "pumpkin_pinata", "display": "Pumpkin Piñata", "hits": 8, "confetti": [0xF08A1C, 0xFFB347, 0x3C8C2C, 0x1C1C1C]},
    "star": {"item": "star_pinata", "display": "Star Piñata", "hits": 10, "confetti": [0xF04C8C, 0xFFD23C, 0x2CC4D8, 0xF0782C]},
    "bat": {"item": "bat_pinata", "display": "Bat Piñata", "hits": 6, "confetti": [0x2A2430, 0x7A3CA8, 0xC8C0D0, 0xE8442C]},
}
# The star's seven cones and their papers: six round its face and one out of the front.
STAR_COLOURS = ("pink", "yellow", "turquoise", "orange", "pink", "yellow", "turquoise")

ADVANCEMENTS = {
    "pinata_party": {"icon": "jugcraft:star_pinata", "title": "Piñata Party", "description": "Burst a piñata", "frame": "task"},
    "blind_luck": {"icon": "jugcraft:blindfold", "title": "Blind Luck", "description": "Burst a piñata while blindfolded", "frame": "goal"},
}

# Papier-mâché: paper and string, and the kind's dye; the stick is painted; the blindfold is a strip of black wool.
SHAPED = [
    {"id": KINDS["pumpkin"]["item"], "pattern": ["PSP", "PDP", " P "],
     "key": {"P": "minecraft:paper", "S": "minecraft:string", "D": "minecraft:orange_dye"}, "result": KINDS["pumpkin"]["item"], "count": 1,
     "category": "misc"},
    {"id": KINDS["star"]["item"], "pattern": ["PSP", "PDP", " P "],
     "key": {"P": "minecraft:paper", "S": "minecraft:string", "D": "minecraft:pink_dye"}, "result": KINDS["star"]["item"], "count": 1,
     "category": "misc"},
    {"id": KINDS["bat"]["item"], "pattern": ["PSP", "PDP", " P "],
     "key": {"P": "minecraft:paper", "S": "minecraft:string", "D": "minecraft:black_dye"}, "result": KINDS["bat"]["item"], "count": 1,
     "category": "misc"},
]
SHAPELESS = [
    {"id": PINATA["stick"], "inputs": ["minecraft:stick", "minecraft:stick", "minecraft:red_dye", "minecraft:white_dye"], "result": PINATA["stick"],
     "count": 1, "category": "equipment"},
    {"id": PINATA["blindfold"], "inputs": ["minecraft:black_wool", "minecraft:string"], "result": PINATA["blindfold"], "count": 1,
     "category": "equipment"},
]


def items():
    return [k["item"] for k in KINDS.values()] + [PINATA["stick"], PINATA["blindfold"]]


def blocks():
    return []


def displays():
    out = {k["item"]: k["display"] for k in KINDS.values()}
    out[PINATA["stick"]] = PINATA["stick_display"]
    out[PINATA["blindfold"]] = PINATA["blindfold_display"]
    return out
