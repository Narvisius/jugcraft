"""11 October owner art correction: readable 16px blades with vanilla's thin item extrusion.

The same authored pixel map is used in inventory and in either hand. The existing ArmsMotion animations still own
poses and attacks. IDs, damage, reach, abilities and recipes are unchanged.
"""

KINDS = {"rapier", "moonblade"}
VARIANTS = {"gilded_rapier": "rapier", "needle_rapier": "rapier", "soulreaver": "moonblade",
            "runebound_moonblade": "moonblade"}


def held_model(kind, texture):
    import arms
    display = {}
    for context, (rotation, translation, scale) in arms.HANDHELD.items():
        factor = 1.15 if kind == "rapier" else (0.95 if context.startswith("firstperson") else 1.65)
        right, left = arms._pose(rotation, translation, scale, (-4.5, -4.5), scale, arms.SWORD_GRIP,
                                 factor)
        display[context] = right
        display[context.replace("righthand", "lefthand")] = left
    return {"parent": "minecraft:item/handheld", "textures": {"layer0": texture}, "display": display}
