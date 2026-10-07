"""JSON resources for the Pumpkling (fall addition 25), from tools/agriculture.py: its name and its tags (the sparks that
wake one, the treats that heal one). The advancement is in HALLOWEEN_ADVANCEMENTS; a Pumpkling has no loot table (it
drops its carved pumpkin itself).

Called from agriculture_data.py. Formats follow vanilla Minecraft 26.3's own files.
"""
from agriculture import PUMPKLING
from decor_data import MOD


def assets(root, write, lang):
    lang[f"entity.{MOD}.{PUMPKLING['entity']}"] = PUMPKLING["display"]


def tags(tags):
    for spark in PUMPKLING["sparks"]:
        tags.add("item", f"{MOD}:pumpkling_sparks", spark)
    for treat in PUMPKLING["treats"]:
        tags.add("item", f"{MOD}:pumpkling_treats", treat)
