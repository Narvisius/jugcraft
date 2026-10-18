"""Portable essence storage and elemental amulets, using the owner's eight icons unchanged."""
MOD = "jugcraft"
def rid(name): return f"{MOD}:{name}"
STARS = dict(zip((f"klein_star_{i}" for i in range(1, 7)), ("Ein", "Zwei", "Drei", "Vier", "Sphere", "Omega")))
CAPACITIES = (4096, 16384, 65536, 262144, 1048576, 4194304)
AMULETS = ("evertide_amulet", "volcanite_amulet")
NAMES = {**{key: f"Klein Star {name}" for key, name in STARS.items()}, "evertide_amulet": "Evertide Amulet", "volcanite_amulet": "Volcanite Amulet"}
TOOLTIPS = {
    **{key: f"Stores up to {capacity:,} essence. Use to store or withdraw; withdraw to your ledger before powering equipment." for key, capacity in zip(STARS, CAPACITIES)},
    "evertide_amulet": "Fills a carried bucket for 64 essence. Active ward: water breathing while submerged, 4 essence per second.",
    "volcanite_amulet": "Fills a carried bucket for 4,096 essence. Active ward: fire resistance while burning or in lava, 8 essence per second.",
}
ITEMS = {key: {"name": name, "tooltip": TOOLTIPS[key]} for key, name in NAMES.items()}
OWNER_FILES = {**{f"textures/item/{key}.png": f"textures/item/stars/{key}.png" for key in STARS},
               **{f"textures/item/{key}.png": f"textures/item/rings/{key}.png" for key in AMULETS}}
LANG = {
    "message.jugcraft.star.research": "Equipment is unavailable: check that Concordance is enabled and its essence data is valid.",
    "tooltip.jugcraft.star.charge": "%s / %s essence stored",
    "tooltip.jugcraft.star.use": "Use: open essence storage. Close to reclaim the star. Upgrade at a smithing table; stored essence is kept.",
    "tooltip.jugcraft.amulet.controls": "Use in air with a plain empty bucket in your other hand. Sneak-use: toggle ward. Hold or wear in a Charm slot. No research required.",
    "message.jugcraft.amulet.toggle": "%s ward: %s",
    "message.jugcraft.amulet.bucket": "Needs a plain empty bucket in the other hand, room for its output, and %s essence in your ledger.",
    "message.jugcraft.amulet.filled": "Bucket filled. %s essence remaining.",
    "screen.jugcraft.star.title": "Klein Star",
    "screen.jugcraft.star.wallet": "Your essence",
    "screen.jugcraft.star.stored": "In this star",
    "screen.jugcraft.star.capacity": "Capacity: %s",
    "screen.jugcraft.star.insert": "Insert a Klein Star to transfer essence",
    "screen.jugcraft.star.inventory": "Your inventory",
    "screen.jugcraft.star.store_help": "Move essence from your personal ledger into this star. Stops at its capacity. No fee.",
    "screen.jugcraft.star.withdraw_help": "Move stored essence into your personal ledger (maximum 2,000,000,000). Equipment spends the ledger. No fee.",
    **{f"screen.jugcraft.star.button_{i}": label for i, label in enumerate(("Store 1,024", "Store 16,384", "Store all", "Take 1,024", "Take 16,384", "Take all"))},
}
SHAPED = {"klein_star_1": (["GLG", "LDL", "GLG"], {"G": "minecraft:gold_ingot", "L": "minecraft:lapis_lazuli", "D": rid("dark_matter")}),
          **{key: (["GDG", "WBW", "GDG"], {"G": "minecraft:gold_ingot", "D": rid("dark_matter"), "W": f"minecraft:{fluid}_bucket", "B": rid("high_covalence_dust")}) for key, fluid in zip(AMULETS, ("water", "lava"))}}

def write_all(write, assets, data, lang, condition, self_drop):
    lang.update(LANG)
    for key in ITEMS:
        lang[f"tooltip.{MOD}.{key}"] = TOOLTIPS[key]
        write(assets / "models/item" / f"{key}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{key}")}})
        write(assets / "items" / f"{key}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{key}")}})
    for key, (pattern, ingredients) in SHAPED.items():
        write(data / "recipe" / f"{key}.json", {"fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": ingredients, "result": {"id": rid(key), "count": 1}})
    for tier in range(2, 7):
        # Vanilla smithing_transform carries the base stack's components to the result, including stored essence.
        write(data / "recipe" / f"klein_star_{tier}.json", {"fabric:load_conditions": condition("concordance"), "type": "minecraft:smithing_transform", "template": rid("high_covalence_dust"), "base": rid(f"klein_star_{tier-1}"), "addition": rid("dark_matter"), "result": {"id": rid(f"klein_star_{tier}"), "count": 1}})

def tags(tags):
    for key in ITEMS: tags.add("item", rid("equivalence/excluded"), rid(key))
    for key in AMULETS: tags.add("item", "trinkets:legs/charm", rid(key))

def codex():
    return {("equivalence", "elemental_gear"): {
        "name": "Stars and Elemental Amulets", "x": 6, "y": 0, "icon": rid("klein_star_1"), "condition": None,
        "description": "Carry essence, supply fluids and protect yourself",
        "pages": [
            ("crafting_recipe", "Klein Star Ein", "**Use** a star to open its storage tablet. Store or take **1,024**, **16,384**, or **all available** essence. Transfers stop at the star's capacity or the personal ledger's **2,000,000,000** limit. Nothing is generated or lost. Closing returns the real star. Equipment spends your personal ledger first, then carried stars in inventory order. Nested stars are not power sources. No research required.", rid("klein_star_1")),
            ("text", "Six Capacities", "**Ein: 4,096**; **Zwei: 16,384**; **Drei: 65,536**; **Vier: 262,144**; **Sphere: 1,048,576**; **Omega: 4,194,304**. At a **smithing table**, put **High Covalence Dust** in the template slot, your current star in the base slot and **Dark Matter** in the addition slot. Each upgrade consumes one dust and one Dark Matter and preserves the star's **charge and custom name**. Filled stars can be traded; on death they follow normal inventory/drop rules. None can be transmuted."),
            ("crafting_recipe", "Evertide Amulet", "Hold a **plain empty bucket** in your other hand and **use in air** to fill one water bucket for **64 essence**. A stack of buckets needs a spare inventory slot. **Sneak-use** switches its ward on/off. Hold the active amulet or equip it in a **Charm** slot for water breathing while submerged, at **4 essence per second**. The ward is short-lived when you run out or remove it. Crafting returns the two empty buckets.", rid("evertide_amulet")),
            ("crafting_recipe", "Volcanite Amulet", "Fills a carried empty bucket with lava for **4,096 essence**. **Sneak-use** enables a ward granting fire resistance while burning or in lava, for **8 essence per second**. Keep a balance before approaching lava; the ward checks once a second and is not a shield against every damage type. Hold or wear in a **Charm** slot. Crafting returns two buckets.", rid("volcanite_amulet")),
            ("text", "Practical Limits", "Wards start **off**, spend essence only while the matching danger is present. Multiple copies do not stack. Existing longer or stronger potions are left alone. Filling a bucket never places fluid in the world: use the ordinary bucket afterward, with normal placement and protection rules. No automatic inventory search, chunk scanning, passive essence generation, flying or new spell system is involved."),
        ]}}

# Owner-requested connected behavior supersedes the original manual-only descriptions.
from concordance_ee_expansion import TOOLTIPS as _connected_tooltips
TOOLTIPS.update({k:v for k,v in _connected_tooltips.items() if k in TOOLTIPS})
