"""Rime, part 2: the Rimekeeper's travel kit (docs/features/arcane-concordance-winter.md; Concordance Chunk B, B-10, owner
decision B-D2): the owner's water sets, foci and bangle (art/owner-library/originals/Magic, imported as supplied by
tools/owner_art.py from OWNER_FILES below), made in survival as Ember's regalia is (tools/concordance_ember.py, part 2).

- The Lesser Focus of Water and the Focus of Water, worn in Ember's Spell Focus slot, are the preservation focus (B-P24,
  B-P50): Preserve holds an Alembic Crucible's heat LESSER_FOCUS_HOLD_TICKS or FOCUS_HOLD_TICKS instead of its own minute.
- The Water Bangle, worn in a Bracelet slot, is the short Brace accessory (B-P25, B-P49): once you understand Rimekeeping,
  it braces you (Brace's own Braced, tools/concordance_strata.py) whenever you have gone BANGLE_RECHARGE_TICKS without a
  Brace. It is nobody's cast: no Stratawrighting practice, no caster's deed.
- Three water sets, worn as the owner's GeckoLib models through Ember's renderer: Nereid's (light) and Aquamancer's
  (medium) on the owner's medium model (imported by Ember as "pyromancers"), Winterguard's (heavy) on its own. Every
  piece is in #minecraft:freeze_immune_wearables, so vanilla's freezing passes its wearer by (powder snow adds no frost,
  a full freeze deals no damage); the Yeti King's frost (lair/yeti/YetiKingEntity.chill) is his own and still slows
  them (B-P23: no permanent boss immunity). No Spell Power, toughness or knockback resistance; protection no more than
  the Pyromaniac's, leather's and iron's (B-P47).

No piece walks on powder snow: vanilla 26.3 keeps that to leather boots (PowderSnowBlock.canEntityWalkOnPowderSnow) and
Jugcraft adds no mixin (B-P46). Java: concordance/rime/RimeGear.java and RimeTrinketItem.java; the sets are Ember's
EmberArmorItem. tools/concordance.py merges these tables; tools/check_mod_data.py check_rime_kit holds the Java and data
to them. Imports only leaf tables (tools/owner_art.py, tools/gear.py, tools/acquisition.py and tools/concordance_journey.py
import it directly).
"""
import json

import concordance_ember as ember
import concordance_rime as rime
import concordance_strata as strata

MOD = "jugcraft"
MAGIC = ember.MAGIC


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------- the preservation focus (B-P24)
LESSER_FOCUS_HOLD_TICKS = rime.LESSER_FOCUS_HOLD_TICKS  # 1800: 90 s
FOCUS_HOLD_TICKS = rime.FOCUS_HOLD_TICKS                # 2400: 120 s
MAX_HOLD_TICKS = 2400  # Java Rime.MAX_HOLD_TICKS = EffectSpec.MAX_DURATION (check_rime_kit: tools/concordance.py EFFECT_LIMITS)

# ------------------------------------------------------------------------------------------ the Water Bangle (B-P49)
BANGLE_RECHARGE_TICKS = 600              # 30 s with no Brace
BANGLE_BRACE_TICKS = strata.BRACE_TICKS  # Brace's own 5 s
BANGLE_CHECK_TICKS = 20                  # each player looked at once a second

# ------------------------------------------------------------------------------------------ the three sets (B-P47)
# Vanilla's protection by slot (mc263src ArmorMaterials.java; makeDefense(boots, leggings, chestplate, helmet)): what the
# medium and heavy sets may not exceed; the light set may not exceed the Pyromaniac's (Ember's light set).
VANILLA_DEFENSE = {"leather": {"helmet": 1, "chestplate": 3, "leggings": 2, "boots": 1},
                   "iron": {"helmet": 2, "chestplate": 6, "leggings": 5, "boots": 2}}
_SHEETS = "assets/ars_jymbaumental/textures/armor"
ARMOR_SETS = {
    "nereids": {"name": "Nereid's", "owner_set": "aqua_light",
                "pieces": {"nereids_hood": "helmet", "nereids_tunic": "chestplate", "nereids_pants": "leggings",
                           "nereids_shoes": "boots"},
                "defense": {"helmet": 1, "chestplate": 2, "leggings": 1, "boots": 1},
                "bound": ember.ARMOR_SETS["pyromaniacs"]["defense"],
                "durability": 7, "enchantability": 15, "sound": "ARMOR_EQUIP_LEATHER", "repair": ["#minecraft:wool"],
                "model": "pyromancers", "texture": f"{_SHEETS}/light_armor_water.png"},
    "aquamancers": {"name": "Aquamancer's", "owner_set": "aqua",
                    "pieces": {"aquamancers_hat": "helmet", "aquamancers_robes": "chestplate",
                               "aquamancers_leggings": "leggings", "aquamancers_boots": "boots"},
                    "defense": {"helmet": 1, "chestplate": 3, "leggings": 2, "boots": 1}, "bound": VANILLA_DEFENSE["leather"],
                    "durability": 10, "enchantability": 15, "sound": "ARMOR_EQUIP_LEATHER", "repair": ["#minecraft:wool"],
                    "model": "pyromancers", "texture": f"{_SHEETS}/medium_armor_water.png"},
    "winterguards": {"name": "Winterguard's", "owner_set": "aqua_heavy",
                     "pieces": {"winterguards_helmet": "helmet", "winterguards_chestplate": "chestplate",
                                "winterguards_leggings": "leggings", "winterguards_boots": "boots"},
                     "defense": {"helmet": 2, "chestplate": 6, "leggings": 5, "boots": 2}, "bound": VANILLA_DEFENSE["iron"],
                     "durability": 15, "enchantability": 9, "sound": "ARMOR_EQUIP_IRON", "repair": ["minecraft:iron_ingot"],
                     "model": "winterguards", "texture": f"{_SHEETS}/heavy_armor_water.png"},
}
ARMOR = {item: piece for armour in ARMOR_SETS.values() for item, piece in armour["pieces"].items()}
SET_OF = {item: name for name, armour in ARMOR_SETS.items() for item in armour["pieces"]}
# The one model this kit imports (the medium model is Ember's import, ember.ARMOR_MODELS). Its head pivots at y 24, as
# the wearer's does, and its bones are Ember's (ARMOR_BONES + SLIM_BONES), so it is copied as supplied.
ARMOR_MODELS = {"winterguards": "assets/ars_jymbaumental/geo/heavy_armor_water.geo.json"}
# Each set's sheet must fit the model it is worn on (check_rime_kit: opaque share of the sheet under the model's faces,
# slim sleeves left out). Measured 14 October 2026: 71% for each set; lowest bone 58% (arms), heavy head 91%; the medium
# sheet on the heavy model 58% overall, head 0%.
MIN_FIT = 0.65
MIN_BONE_FIT = 0.5


def repair_tag(armour_set):
    return rid(f"repairs_{armour_set}_gear")


# Worn in Ember's two slots (ember.TRINKET_SLOTS); tools/concordance.py adds these to ember.SLOT_ITEMS (B-P51).
TRINKETS = {"lesser_water_focus": "chest/spell_focus", "water_focus": "chest/spell_focus", "water_bangle": "hand/bracelet"}

OWNER_ITEMS = {
    "lesser_water_focus": "lesser_water_focus", "water_focus": "water_focus", "water_bangle": "water_bangle",
    "nereids_hood": "aqua_hood", "nereids_tunic": "aqua_tunic", "nereids_pants": "aqua_pants", "nereids_shoes": "aqua_shoes",
    "aquamancers_hat": "aqua_hat", "aquamancers_robes": "aqua_robes", "aquamancers_leggings": "aqua_leggings",
    "aquamancers_boots": "aqua_boots",
    "winterguards_helmet": "aqua_helmet", "winterguards_chestplate": "aqua_chestplate",
    "winterguards_leggings": "aqua_leggings_heavy", "winterguards_boots": "aqua_boots_heavy",
}
OWNER_LANG = ember.OWNER_LANG  # names only (PROVENANCE.md:14): nothing else in that file is taken

_HOLD = rime.PRESERVE_HOLD_TICKS // 20
ITEMS = {
    "lesser_water_focus": {"name": "Lesser Focus of Water",
                           "tooltip": f"Worn in the Spell Focus slot: Preserve holds a crucible's heat {LESSER_FOCUS_HOLD_TICKS // 20} "
                                      f"seconds instead of {_HOLD}. It gives no Spell Power and holds no Focus."},
    "water_focus": {"name": "Focus of Water",
                    "tooltip": f"Worn in the Spell Focus slot: Preserve holds a crucible's heat {FOCUS_HOLD_TICKS // 20} seconds "
                               f"instead of {_HOLD}. It gives no Spell Power and holds no Focus."},
    "water_bangle": {"name": "Water Bangle",
                     "tooltip": f"Worn in a Bracelet slot: once you understand Rimekeeping, whenever you have gone "
                                f"{BANGLE_RECHARGE_TICKS // 20} seconds without a Brace it braces you for {BANGLE_BRACE_TICKS // 20} "
                                "seconds (the next push moves you half as far). A second bangle adds nothing."},
    "nereids_hood": {"name": "Nereid's Hood"}, "nereids_tunic": {"name": "Nereid's Tunic"},
    "nereids_pants": {"name": "Nereid's Pants"}, "nereids_shoes": {"name": "Nereid's Shoes"},
    "aquamancers_hat": {"name": "Aquamancer's Hat"}, "aquamancers_robes": {"name": "Aquamancer's Robes"},
    "aquamancers_leggings": {"name": "Aquamancer's Leggings"}, "aquamancers_boots": {"name": "Aquamancer's Boots"},
    "winterguards_helmet": {"name": "Winterguard's Helmet"}, "winterguards_chestplate": {"name": "Winterguard's Chestplate"},
    "winterguards_leggings": {"name": "Winterguard's Leggings"}, "winterguards_boots": {"name": "Winterguard's Boots"},
}
FROST_LINE = "The Yeti King's frost still slows you."
BOOTS_LINE = "They still sink in powder snow: only leather boots walk on it."
for _piece, _set in SET_OF.items():
    ITEMS[_piece]["tooltip"] = (f"The {ARMOR_SETS[_set]['name']} set, for cold travel: while you wear any piece, powder snow "
                                f"and hard cold never freeze or hurt you. {FROST_LINE}"
                                + (f" {BOOTS_LINE}" if ARMOR[_piece] == "boots" else ""))
BLOCKS = {}

# Shaped crafting from Overworld things had before the Adept stage (B-P48): Ember's patterns with a cold accent. The
# owner's own recipes use another mod's machines, netherite and water essence, so they are not imported.
_NEREIDS = {"W": "#minecraft:wool", "S": "minecraft:snowball"}
_AQUAMANCERS = {"W": "#minecraft:wool", "L": "minecraft:lapis_lazuli"}
_WINTERGUARDS = {"I": "minecraft:iron_ingot", "B": "minecraft:snow_block"}
RECIPES = {
    "lesser_water_focus": {"pattern": ["SGS", "NAN", " G "],
                           "key": {"S": "minecraft:string", "G": "minecraft:gold_ingot", "N": "minecraft:snowball",
                                   "A": "minecraft:amethyst_shard"}},
    "water_focus": {"pattern": [" D ", "GLG", " D "],
                    "key": {"D": "minecraft:diamond", "G": "minecraft:gold_ingot", "L": rid("lesser_water_focus")}},
    "water_bangle": {"pattern": ["GBG", "G G", " A "],
                     "key": {"G": "minecraft:gold_ingot", "B": "minecraft:snow_block", "A": "minecraft:amethyst_shard"}},
    "nereids_hood": {"pattern": ["WSW", "W W"], "key": _NEREIDS},
    "nereids_tunic": {"pattern": ["W W", "WSW", "WWW"], "key": _NEREIDS},
    "nereids_pants": {"pattern": ["WSW", "W W", "W W"], "key": _NEREIDS},
    "nereids_shoes": {"pattern": ["W W", "S S"], "key": _NEREIDS},
    "aquamancers_hat": {"pattern": ["WLW", "W W"], "key": _AQUAMANCERS},
    "aquamancers_robes": {"pattern": ["W W", "WLW", "WWW"], "key": _AQUAMANCERS},
    "aquamancers_leggings": {"pattern": ["WLW", "W W", "W W"], "key": _AQUAMANCERS},
    "aquamancers_boots": {"pattern": ["W W", "L L"], "key": _AQUAMANCERS},
    # Iron armour's iron (5, 8, 7, 4) and a snow block in the cell vanilla leaves empty: never cheaper than iron.
    "winterguards_helmet": {"pattern": ["III", "IBI"], "key": _WINTERGUARDS},
    "winterguards_chestplate": {"pattern": ["IBI", "III", "III"], "key": _WINTERGUARDS},
    "winterguards_leggings": {"pattern": ["III", "IBI", "I I"], "key": _WINTERGUARDS},
    "winterguards_boots": {"pattern": ["IBI", "I I"], "key": _WINTERGUARDS},
}

# Every owner file the kit uses, copied byte for byte by tools/owner_art.py (the model's line ends are already LF).
OWNER_FILES = {
    **{f"textures/item/{item}.png": f"assets/ars_jymbaumental/textures/item/{owner}.png" for item, owner in OWNER_ITEMS.items()},
    **{f"textures/armor/{name}.png": armour["texture"] for name, armour in ARMOR_SETS.items()},
    **{f"geckolib/models/armor/{model}.geo.json": source for model, source in ARMOR_MODELS.items()},
}


def owner_lang():
    return ember.owner_lang()


def owner_model(item):
    """The owner's item model for `item`, its texture renamed to the Jugcraft copy; anything but the plain generated
    model stops the generator (a changed library file is a decision, not a merge)."""
    owner = OWNER_ITEMS[item]
    model = json.loads((MAGIC / "assets" / "ars_jymbaumental" / "models" / "item" / f"{owner}.json").read_text(encoding="utf-8-sig"))
    if model != {"parent": "minecraft:item/generated", "textures": {"layer0": f"ars_jymbaumental:item/{owner}"}}:
        raise SystemExit(f"tools/concordance_rime_kit.py: the owner's model for {owner} is not the plain generated model expected")
    return {"parent": model["parent"], "textures": {"layer0": rid(f"item/{item}")}}


def codex():
    total = {name: sum(armour["defense"].values()) for name, armour in ARMOR_SETS.items()}
    return {("winter", "travel_kit"): {
        "name": "A Rimekeeper's Travel Kit", "x": 6, "y": 0, "icon": rid("water_focus"), "condition": None,
        "description": "Cold travel, worn",
        "pages": [
            ("text", "A Rimekeeper's Travel Kit",
             "For cold country: a **focus of water** for Preserve, a **Water Bangle** that braces you, and three water "
             "sets. While you wear **any piece of any set**, powder snow and hard cold never freeze or hurt you; you "
             "still sink in powder snow (only leather boots walk on it). The **Yeti King's frost** is his own: in any set "
             "it still slows you; Frostward (Winter Brine) or his Mitten keeps it off. Nothing here gives Spell Power or "
             "holds Focus."),
            ("crafting_recipe", "Lesser Focus of Water",
             f"In the Spell Focus slot, Preserve holds {LESSER_FOCUS_HOLD_TICKS // 20} seconds instead of {_HOLD}. One "
             "focus at a time: it takes the Focus of Fire's slot.", rid("lesser_water_focus")),
            ("crafting_recipe", "Focus of Water",
             f"The lesser focus set with diamonds: Preserve holds {FOCUS_HOLD_TICKS // 20} seconds.", rid("water_focus")),
            ("crafting_recipe", "Water Bangle",
             f"Once you understand Rimekeeping, it braces you whenever you have gone {BANGLE_RECHARGE_TICKS // 20} seconds "
             f"without a Brace: for {BANGLE_BRACE_TICKS // 20} seconds the next push moves you half as far. It is not your "
             "cast, so it teaches Stratawrighting nothing; a second bangle adds nothing.", rid("water_bangle")),
            ("text", "Three Water Sets",
             f"**Nereid's** (cloth and snow, {total['nereids']} protection) is the Pyromaniac's equal; **Aquamancer's** "
             f"(cloth and lapis, {total['aquamancers']}) leather's, and lasts longer; **Winterguard's** (iron and snow, "
             f"{total['winterguards']}) iron's. None is stronger than what it matches: what each adds is the cold kept out."),
            *[("crafting_recipe", ITEMS[item]["name"],
               f"{ARMOR_SETS[SET_OF[item]]['name']} set: {total[SET_OF[item]]} protection for the whole set.", rid(item))
              for item in ARMOR],
        ]}}


def tags(tags):
    for item in ARMOR:
        tags.add("item", "minecraft:freeze_immune_wearables", rid(item))


def write_all(write, assets, data, lang, condition, self_drop):
    for item in ITEMS:
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        write(assets / "models" / "item" / f"{item}.json", owner_model(item))
    for item, recipe in RECIPES.items():
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": recipe["pattern"], "key": recipe["key"], "result": {"id": rid(item), "count": 1}})
    for armour_set, armour in ARMOR_SETS.items():
        write(data / "tags" / "item" / f"{repair_tag(armour_set).split(':')[1]}.json", {"values": armour["repair"]})
