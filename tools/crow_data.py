"""JSON resources for crows (fall additions 4), from tools/agriculture.py: the crow's name and its loot (a few feathers).

Called from agriculture_data.py (assets, loot). The crow is drawn by the client's CrowRenderer and CrowModel (texture
from tools/crow_textures.py); scarecrows need no new resources (they guard by what they already wear).
"""
from agriculture import CROWS
from decor_data import MOD, rid


def assets(root, write, lang):
    lang[f"entity.{MOD}.{CROWS['entity']}"] = CROWS["display"]


def loot(out, write):
    """A crow drops 0 to a few feathers (out = loot_table/blocks; the table goes beside it under entities)."""
    write(out.parent / f"{CROWS['table']}.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "minecraft:feather",
        "modifier": {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": CROWS["feathers"]}}}]}],
        "random_sequence": rid(CROWS["table"])})
