"""Textures for roadmap step 16, the Crimson Vigil (tools/concordance_crimson.py): the Crimson Chalice's and Thornheart
Blade's item icons, drawn from their 16x16 maps in tools/item_icons/ (docs/ITEM_ICONS.md). tools/concordance_art.py
includes these in its textures().

Provenance (docs/features/arcane-concordance-vitae.md, "Art"): both icons are drawn fresh as maps; the blade's map takes
the outline of Jugcraft's own greatsword map (tools/arms_icons/greatsword.txt), with crimson veins added. Nothing is taken from the owner's library
or from Mojang's files.
"""

import item_icons

ICONS = ["crimson_chalice", "thornheart_blade"]


def textures():
    return {("item", name): item_icons.draw(name) for name in ICONS}
