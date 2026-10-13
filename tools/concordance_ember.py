"""Ember, the Hearthbinders' Principle (docs/features/arcane-concordance-ember.md), part 1: research and invocations.

The Hearthbinders keep fire and use it with control: heat for hearths, kilns and forges, never wildfire. Part 1 is
their research entry, Hearthbinding, and four invocations written in the shared grammar (roadmap step 10's rules):

- Hearthspark (utility): kindles an unlit campfire, candle or candle cake where you look, as flint and steel would;
- Hearthguard (defense): Fire Resistance;
- Cinderbolt (damage): fire damage that grows with fire Spell Power, and Smoulder;
- Hearthflare (damage, once mastered): a burst of fire round you, and Smoulder.

What Ember adds to the shared effects is small (Java: concordance/ember): an alteration in Ember's Spell Power school
kindles a hearth instead of putting out fire, and the Smoulder status keeps a creature alight while it lasts (vanilla
burning: water, rain, Fire Resistance and fire immunity all answer it). Nothing here places fire in the world.

Part 2 (docs/features/arcane-concordance-ember-regalia.md) is the Hearthbinder's regalia, made from the owner's own fire
art (art/owner-library/originals/Magic, imported as supplied by tools/owner_art.py from OWNER_FILES below): the Lesser
Focus of Fire and the Focus of Fire (fire Spell Power, worn in a Spell Focus slot), the Fire Bangle (a Hearthbinder's
melee blows leave the creature smouldering; worn in a Bracelet slot), and two fire sets, half a point of fire Spell Power
a piece, both worn as the owner's medium GeckoLib model in their own textures: the Pyromaniac's Hood, Tunic, Pants and
Shoes (light: cloth, in the owner's light fire texture) and the Pyromancer's Hat, Robes, Leggings and Boots (medium:
leather's protection). The two Trinkets
slots are the owner's own (their Curios data and slot icons, ported). Hearthflare's release is the owner's "pyro"
recordings. Java: concordance/ember/EmberGear.java and the client's EmberClient.

tools/concordance.py merges these tables into its own. Numbers the Java repeats are checked by tools/check_mod_data.py.
"""
import json
from pathlib import Path

MOD = "jugcraft"
# The owner's magic collection (art/owner-library/MAGIC_ASSETS.md): read here, never written.
MAGIC = Path(__file__).resolve().parents[1] / "art" / "owner-library" / "originals" / "Magic"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# What can be examined: fuels and things that hold fire. Coal, charcoal, torches and campfires are had in the Overworld
# from the start (charcoal from any log), so the first three are never out of reach; the rest come from the Nether or
# the sea floor.
EMBER_SPECIMEN_TAG = f"{MOD}:ember_specimens"
EMBER_SPECIMENS = {
    "minecraft:coal": "Coal", "minecraft:charcoal": "Charcoal", "minecraft:torch": "Torch",
    "minecraft:campfire": "Campfire", "minecraft:magma_block": "Magma Block", "minecraft:blaze_powder": "Blaze Powder",
    "minecraft:blaze_rod": "Blaze Rod", "minecraft:magma_cream": "Magma Cream", "minecraft:fire_charge": "Fire Charge",
}
# Different specimens examined to observe Hearthbinding.
OBSERVED_DISTINCT = 3

# The practice Hearthbinding is mastered by (Java: ember/Ember.ACTIVITY): each kind of hearth kindled with Hearthspark
# counts once (Java: Ember.hearth). Three of the four are had in the Overworld; the soul campfire needs the Nether.
HEARTHKEEPING = rid("hearthkeeping")
HEARTHS = {"campfire": "a campfire", "soul_campfire": "a soul campfire", "candle": "a candle",
           "candle_cake": "a candle cake"}
HEARTH_MASTERY = 3

# Smoulder (Java: ember/SmoulderEffect): whenever the creature's fire has burnt out, it is relit for FIRE_TICKS (a
# second), so it burns at vanilla's pace, a point of damage a second, while the status lasts.
SMOULDER = rid("smoulder")
SMOULDER_FIRE_TICKS = 20
SMOULDER_COLOUR = 0xF0743C
SMOULDER_TICKS = 60  # Cinderbolt's and Hearthflare's

# Hearthguard's Fire Resistance.
HEARTHGUARD_TICKS = 600
# Hearthspark's reach (its delivery's: the ray).
HEARTHSPARK_RANGE = 16

# ------------------------------------------------------------------------------------------------- research

_UNDERSTOOD = {"research": rid("hearthbinding"), "state": "understood"}
_MASTERED = {"research": rid("hearthbinding"), "state": "mastered"}

RESEARCH = {
    "hearthbinding": {
        "name": "Hearthbinding",
        "principle": "ember",
        "tradition": "hearthbinders",
        "stage": "practitioner",
        "icon": "minecraft:campfire",
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{EMBER_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{EMBER_SPECIMEN_TAG}", "distinct": OBSERVED_DISTINCT}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{EMBER_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: different kinds of hearth kindled.
            "mastered": [{"type": "practice", "activity": HEARTHKEEPING, "distinct": HEARTH_MASTERY}],
        },
        "unlocks": {"understood": {"invocations": [rid("hearthspark"), rid("hearthguard"), rid("cinderbolt")]},
                    "mastered": {"invocations": [rid("hearthflare")]}},
        "advancements": {
            "encountered": ("Banked Coals", "Examine something that holds fire", "task", "minecraft:coal"),
            "observed": ("Tinder and Flame", f"Examine {OBSERVED_DISTINCT} different things that hold fire", "task",
                         "minecraft:torch"),
            "understood": ("Hearthbinding",
                           "Understand Hearthbinding: study something that holds fire at a Lampwright's Bench",
                           "goal", "minecraft:campfire"),
            "mastered": ("Hearthbinder", f"Kindle {HEARTH_MASTERY} different kinds of hearth with Hearthspark",
                         "challenge", "minecraft:blaze_powder"),
        },
        "locked": {"encountered": "Examine something that holds fire first",
                   "observed": f"Examine {OBSERVED_DISTINCT} different things that hold fire first",
                   "understood": "Understand Hearthbinding first", "mastered": "Master Hearthbinding first"},
    },
}

# ------------------------------------------------------------------------------------- components and invocations

# Authored words: the invocations below are written with them, and players cannot compose with them (roadmap step 10).
COMPONENTS = {
    "kindling": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 2,
                 "operation": {"effect": "alteration", "intent": "helpful", "principle": "ember",
                               "school": "spell_power:fire"},
                 "name": "Kindling", "text": "Hearthspark's spark: kindles an unlit campfire, candle or candle cake."},
    "hearthguard_ward": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 2, "focus": 4,
                         "operation": {"effect": "status", "intent": "helpful", "principle": "ember", "magnitude": 0,
                                       "duration": HEARTHGUARD_TICKS, "status": "minecraft:fire_resistance"},
                         "name": "Banked Fire", "text": "Hearthguard's warding: Fire Resistance."},
    "cinder": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 2, "focus": 3,
               "operation": {"effect": "damage", "intent": "harmful", "principle": "ember", "magnitude": 3,
                             "school": "spell_power:fire", "scaling": 0.5},
               "name": "Cinder", "text": "Cinderbolt's burning shard: fire damage that grows with fire Spell Power."},
    "smoulder": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 1,
                 "operation": {"effect": "status", "intent": "harmful", "principle": "ember", "magnitude": 0,
                               "duration": SMOULDER_TICKS, "status": SMOULDER},
                 "name": "Smoulder", "text": "Leaves the creature burning for a few seconds (Smoulder)."},
    "flare": {"slot": "operation", "requires": _MASTERED, "authored": True, "capacity": 2, "focus": 4,
              "operation": {"effect": "damage", "intent": "harmful", "principle": "ember", "magnitude": 4,
                            "school": "spell_power:fire", "scaling": 0.5},
              "name": "Flare", "text": "Hearthflare's burst: fire damage that grows with fire Spell Power."},
}

INVOCATIONS = {
    "hearthspark": {
        "name": "Hearthspark", "role": "utility", "composition": "ray struck kindling", "stage": "understood",
        "focus": 3, "mastered_focus": 2, "tunings": ["extend"], "work": 4, "persists": 0,
        "cast": 0.5, "cooldown": 1.5,
        "animation": ("kindle_cast", "kindle_release"), "sounds": ("concordance.ember_gather", "concordance.hearthspark"),
        "particle": "minecraft:flame", "icon": "minecraft:flint_and_steel",
        "description": f"A spark that kindles the unlit campfire, candle or candle cake you look at, up to "
                       f"{HEARTHSPARK_RANGE} blocks away.",
        "counter": "It kindles only hearths you could light by hand there, never sets anything else alight, and a "
                   "waterlogged hearth will not catch.",
        "fizzle": "Nothing there to kindle: Hearthspark needs an unlit campfire, candle or candle cake within %s blocks",
    },
    "hearthguard": {
        "name": "Hearthguard", "role": "defense", "composition": "here struck hearthguard_ward", "stage": "understood",
        "focus": 5, "mastered_focus": 4, "tunings": ["prolong"], "work": 1, "persists": 2 * HEARTHGUARD_TICKS,
        "cast": 0.6, "cooldown": 20.0,
        "animation": ("aegis_cast", "aegis_release"), "sounds": ("concordance.ember_gather", "concordance.hearthguard"),
        "particle": "minecraft:small_flame", "icon": "minecraft:magma_cream",
        "description": f"Banks the fire round you: Fire Resistance for {HEARTHGUARD_TICKS // 20} seconds.",
        "counter": "Milk clears it, it guards against fire and lava only, and the cooldown is long.",
        "fizzle": "The fire finds nothing to guard",
    },
    "cinderbolt": {
        "name": "Cinderbolt", "role": "damage", "composition": "ray struck cinder smoulder", "stage": "understood",
        "focus": 5, "mastered_focus": 4, "tunings": ["extend", "intensify"], "work": 3, "persists": SMOULDER_TICKS,
        "cast": 0.5, "cooldown": 2.0,
        "animation": ("lance_cast", "lance_release"), "sounds": ("concordance.ember_gather", "concordance.cinderbolt"),
        "particle": "minecraft:flame", "icon": "minecraft:fire_charge",
        "description": "A burning shard: fire damage to the first creature it meets, which then smoulders. Stronger "
                       "with fire Spell Power.",
        "counter": "It needs a clear line and strikes one target; water, rain and Fire Resistance put out the "
                   "smoulder, and fire-born creatures shrug it off.",
        "fizzle": "The shard meets nothing to burn",
    },
    "hearthflare": {
        "name": "Hearthflare", "role": "damage", "composition": "here creatures flare smoulder", "stage": "mastered",
        "focus": 7, "mastered_focus": 6, "tunings": ["widen", "intensify"], "work": 8, "persists": SMOULDER_TICKS,
        "cast": 0.8, "cooldown": 10.0,
        "animation": ("ward_cast", "ward_release"), "sounds": ("concordance.ember_gather", "concordance.hearthflare"),
        "particle": "minecraft:lava", "icon": "minecraft:blaze_powder",
        "description": "A burst of fire round you: fire damage to the nearest creatures, which then smoulder. Never "
                       "you; stronger with fire Spell Power.",
        "counter": "It reaches only 3 blocks, the cast can be seen coming, and it spares no one but you: keep allies "
                   "clear (it never harms players you may not harm).",
        "fizzle": "No creature stands close enough to burn",
    },
}
for _invocation in INVOCATIONS.values():
    _invocation.update({"principle": "ember", "research": rid("hearthbinding")})

# Status names for the codex's words (vanilla's and Jugcraft's).
STATUS_NAMES = {"minecraft:fire_resistance": "Fire Resistance", SMOULDER: "Smoulder"}

# ------------------------------------------------------------------------------------------------ words and sounds

MESSAGES = {
    "examine.heat": "The %s holds fire. You turn it over, feeling for the heat it keeps.",
}

SOUND_EVENTS = {
    "concordance.ember_gather": "Embers gather",
    "concordance.hearthspark": "Spark kindles",
    "concordance.hearthguard": "Fire banks round you",
    "concordance.cinderbolt": "Cinder flies",
    "concordance.hearthflare": "Fire bursts",
}
# Events played from the owner's recordings instead of a cue drawn by tools/concordance_sounds.py: event -> its sound
# files under assets/jugcraft/sounds/ (copied by tools/owner_art.py, OWNER_FILES). Hearthflare's release is the owner's
# four "pyro" fire roars, as their own fire_family_2 event plays them (ars_jimbaux/sounds.json).
SOUND_FILES = {"concordance.hearthflare": [f"concordance/pyro_{n}" for n in range(1, 5)]}

# ------------------------------------------------------------------------------- part 2: the Hearthbinder's regalia

# Fire Spell Power (Spell Power's spell_power:fire attribute), in points above the school's base. Cinderbolt and
# Hearthflare add half a point of damage for each whole point (floor(0.5 x points): two points make one more damage).
# The most a player wears with the default slots is a Focus of Fire and the whole Pyromancer's set: +6, as much as the
# best arcane Resonant Ring a player can forge (tools/concordance_artifice.py: 2 + 1 + 3), so Cinderbolt deals at most 6
# and Hearthflare 7. Java: ember/EmberGear.java.
LESSER_FOCUS_POWER = 2.0
FOCUS_POWER = 4.0
ROBE_PIECE_POWER = 0.5  # all four pieces: +2, one more damage; fewer than four add none (whole points only)
# The Fire Bangle's blow: a Hearthbinder's melee hit leaves the creature smouldering this long (Arms' Ember boon on a
# weapon sets a struck creature alight for the same three seconds).
BANGLE_SMOULDER_TICKS = 60
# ...and only within this many blocks beyond the reach of the weapon in hand (eye to the creature's hitbox, as vanilla
# measures a melee hit), so a Shock arc's second foe or a weapon skill landing farther off is not set smouldering.
BANGLE_REACH_MARGIN = 1.0

# The two fire sets, each its own items, protection and worn texture; each piece gives ROBE_PIECE_POWER fire Spell Power
# in its slot, so a whole set (or any four pieces) adds a point of damage. Durability is vanilla's per-slot base times
# "durability"; both have leather's enchantability and are repaired with wool.
# - the Pyromaniac's (light): cloth only, a little less protection than leather (1/2/1/1, 5);
# - the Pyromancer's (medium): wool and gold, leather's protection (1/3/2/1, 7), longer-lasting.
# Both are worn as the owner's medium model, "pyromancers" (assets/jugcraft/geckolib/models/armor/<model>.geo.json): the
# owner's light fire texture is drawn for that model (it is the medium sheet, byte for byte, as their air, earth and water
# light textures are laid out for it too), so the light set wears it there. Each set's texture is textures/armor/<set>.png
# and its equipment asset jugcraft:<set> (no equipment JSON: GeckoLib draws the pieces, so vanilla draws no flat layer).
ARMOR_SETS = {
    "pyromaniacs": {"name": "Pyromaniac's",
                    "pieces": {"pyromaniacs_hood": "helmet", "pyromaniacs_tunic": "chestplate", "pyromaniacs_pants": "leggings",
                               "pyromaniacs_shoes": "boots"},
                    "defense": {"helmet": 1, "chestplate": 2, "leggings": 1, "boots": 1}, "durability": 7,
                    "model": "pyromancers", "texture": "assets/ars_jymbaumental/textures/armor/light_armor_fire.png"},
    "pyromancers": {"name": "Pyromancer's",
                    "pieces": {"pyromancers_hat": "helmet", "pyromancers_robes": "chestplate", "pyromancers_leggings": "leggings",
                               "pyromancers_boots": "boots"},
                    "defense": {"helmet": 1, "chestplate": 3, "leggings": 2, "boots": 1}, "durability": 10,
                    "model": "pyromancers", "texture": "assets/ars_jymbaumental/textures/armor/medium_armor_fire.png"},
}
ARMOR_ENCHANTABILITY = 15
# Every piece of both sets: item -> armour piece.
ARMOR = {item: piece for armour in ARMOR_SETS.values() for item, piece in armour["pieces"].items()}
SET_OF = {item: name for name, armour in ARMOR_SETS.items() for item in armour["pieces"]}
# The owner's models the sets are worn as: model -> its file under originals/Magic.
ARMOR_MODELS = {"pyromancers": "assets/ars_jymbaumental/geo/medium_armor_e.geo.json"}
# The model's bones GeckoLib poses to the wearer (GeoArmorRenderer.getBoneNameForSegment), and the slim sleeves the
# client hides (vanilla draws the same sleeves for both arm widths).
ARMOR_BONES = ["armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg",
               "armorRightBoot", "armorLeftBoot"]
SLIM_BONES = ["armorRightArmSlim", "armorLeftArmSlim"]


def repair_tag(armour_set):
    return rid(f"repairs_{armour_set}_gear")


# The owner's two slots, ported from their Curios data to Trinkets slots players get (data/trinkets/slots/<group>/<slot>
# .json): an_focus ("Spell Focus", one) and bracelet (two, the bangle's). Each draws the owner's own slot icon.
TRINKET_SLOTS = {
    "chest/spell_focus": {"name": "Spell Focus", "amount": 1, "icon": "spell_focus",
                          "source": "data/ars_jymbaumental/curios/slots/an_focus.json"},
    "hand/bracelet": {"name": "Bracelet", "amount": 2, "icon": "bracelet",
                      "source": "data/ars_jymbaumental/curios/slots/bracelet.json"},
}
TRINKETS = {"lesser_fire_focus": "chest/spell_focus", "fire_focus": "chest/spell_focus", "fire_bangle": "hand/bracelet"}
FIRE_POWER = {"lesser_fire_focus": LESSER_FOCUS_POWER, "fire_focus": FOCUS_POWER,
              **{piece: ROBE_PIECE_POWER for piece in ARMOR}}

# Each item's owner id (the owner's file names under assets/ars_jymbaumental) and display name (the owner's, from their
# lang file: only these names are taken from it; the tooltips are Jugcraft's own).
OWNER_ITEMS = {
    "lesser_fire_focus": "lesser_fire_focus", "fire_focus": "fire_focus", "fire_bangle": "fire_bangle",
    "pyromaniacs_hood": "fire_hood", "pyromaniacs_tunic": "fire_tunic", "pyromaniacs_pants": "fire_pants",
    "pyromaniacs_shoes": "fire_shoes",
    "pyromancers_hat": "fire_hat", "pyromancers_robes": "fire_robes", "pyromancers_leggings": "fire_leggings",
    "pyromancers_boots": "fire_boots",
}
OWNER_LANG = "assets/ars_jymbaumental/lang/en_us.json"

ITEMS = {
    "lesser_fire_focus": {
        "name": "Lesser Focus of Fire",
        "tooltip": f"Worn in the Spell Focus slot: +{LESSER_FOCUS_POWER:g} fire Spell Power, so Cinderbolt and Hearthflare "
                   "burn hotter. It holds no Focus."},
    "fire_focus": {
        "name": "Focus of Fire",
        "tooltip": f"Worn in the Spell Focus slot: +{FOCUS_POWER:g} fire Spell Power, so Cinderbolt and Hearthflare burn "
                   "hotter. It holds no Focus."},
    "fire_bangle": {
        "name": "Fire Bangle",
        "tooltip": f"Worn in a Bracelet slot: once you understand Hearthbinding, your melee blows leave the creature "
                   f"smouldering for {BANGLE_SMOULDER_TICKS // 20} seconds."},
    "pyromaniacs_hood": {"name": "Pyromaniac's Hood"},
    "pyromaniacs_tunic": {"name": "Pyromaniac's Tunic"},
    "pyromaniacs_pants": {"name": "Pyromaniac's Pants"},
    "pyromaniacs_shoes": {"name": "Pyromaniac's Shoes"},
    "pyromancers_hat": {"name": "Pyromancer's Hat"},
    "pyromancers_robes": {"name": "Pyromancer's Robes"},
    "pyromancers_leggings": {"name": "Pyromancer's Leggings"},
    "pyromancers_boots": {"name": "Pyromancer's Boots"},
}
for _piece, _set in SET_OF.items():
    ITEMS[_piece]["tooltip"] = (f"The {ARMOR_SETS[_set]['name']} regalia: half a point of fire Spell Power. Four pieces "
                                "together add a point to Cinderbolt and Hearthflare.")
BLOCKS = {}

# Shaped crafting from Overworld materials (any time: the regalia's power matters only to Hearthbinding's invocations,
# and the bangle's blow needs Hearthbinding understood). The owner's own recipes use another mod's machines and Nether
# or End items, so they are not imported. The light set is cloth (wool in leather armour's shapes); the medium set is
# wool and gold.
_WOOL = {"W": "#minecraft:wool"}
_WOOL_GOLD = {"W": "#minecraft:wool", "G": "minecraft:gold_ingot"}
RECIPES = {
    "lesser_fire_focus": {"pattern": ["SGS", "CAC", " G "],
                          "key": {"S": "minecraft:string", "G": "minecraft:gold_ingot", "C": "#minecraft:coals",
                                  "A": "minecraft:amethyst_shard"}},
    "fire_focus": {"pattern": [" D ", "GLG", " D "],
                   "key": {"D": "minecraft:diamond", "G": "minecraft:gold_ingot", "L": rid("lesser_fire_focus")}},
    "fire_bangle": {"pattern": ["GCG", "G G", " A "],
                    "key": {"G": "minecraft:gold_ingot", "C": "#minecraft:coals", "A": "minecraft:amethyst_shard"}},
    "pyromaniacs_hood": {"pattern": ["WWW", "W W"], "key": _WOOL},
    "pyromaniacs_tunic": {"pattern": ["W W", "WWW", "WWW"], "key": _WOOL},
    "pyromaniacs_pants": {"pattern": ["WWW", "W W", "W W"], "key": _WOOL},
    "pyromaniacs_shoes": {"pattern": ["W W", "W W"], "key": _WOOL},
    "pyromancers_hat": {"pattern": ["WGW", "W W"], "key": _WOOL_GOLD},
    "pyromancers_robes": {"pattern": ["W W", "WGW", "WWW"], "key": _WOOL_GOLD},
    "pyromancers_leggings": {"pattern": ["WGW", "W W", "W W"], "key": _WOOL_GOLD},
    "pyromancers_boots": {"pattern": ["W W", "G G"], "key": _WOOL_GOLD},
}

# Every owner file this slice uses, copied as supplied by tools/owner_art.py: runtime path under assets/jugcraft ->
# path under originals/Magic. The only changes are the names (Jugcraft ids) and, for the geo model's JSON, its line ends
# (LF, as Git stores the mod's text).
OWNER_FILES = {
    **{f"textures/item/{item}.png": f"assets/ars_jymbaumental/textures/item/{owner}.png" for item, owner in OWNER_ITEMS.items()},
    **{f"textures/armor/{name}.png": armour["texture"] for name, armour in ARMOR_SETS.items()},
    **{f"geckolib/models/armor/{model}.geo.json": source for model, source in ARMOR_MODELS.items()},
    **{f"textures/gui/sprites/container/slots/{info['icon']}.png": f"assets/curios/textures/slot/{owner}.png"
       for info, owner in ((TRINKET_SLOTS["chest/spell_focus"], "an_focus_slot"), (TRINKET_SLOTS["hand/bracelet"], "bangle_slot"))},
    **{f"sounds/{name}.ogg": f"assets/ars_jimbaux/sounds/{name.split('/')[1]}.ogg"
       for names in SOUND_FILES.values() for name in names},
}


def owner_lang():
    """The owner's language file, read for the regalia's display names only."""
    return json.loads((MAGIC / OWNER_LANG).read_text(encoding="utf-8-sig"))


def owner_model(item):
    """The owner's item model for `item`, with its texture renamed to the Jugcraft copy. It must be the plain generated
    model the owner supplied; anything else stops the generator (a changed library file is a decision, not a merge)."""
    owner = OWNER_ITEMS[item]
    model = json.loads((MAGIC / "assets" / "ars_jymbaumental" / "models" / "item" / f"{owner}.json").read_text(encoding="utf-8-sig"))
    if model != {"parent": "minecraft:item/generated", "textures": {"layer0": f"ars_jymbaumental:item/{owner}"}}:
        raise SystemExit(f"tools/concordance_ember.py: the owner's model for {owner} is not the plain generated model expected")
    return {"parent": model["parent"], "textures": {"layer0": rid(f"item/{item}")}}


def lang_entries(lang):
    lang[f"effect.{MOD}.smoulder"] = "Smoulder"
    lang[f"tag.item.{MOD}.ember_specimens"] = "Things That Hold Fire"
    for slot, info in TRINKET_SLOTS.items():
        lang[f"trinkets.slot.{slot.replace('/', '.')}"] = info["name"]


def _hearth_list():
    return ", ".join(HEARTHS.values())


def codex():
    research = "hearthbinding"
    specimens = "\\\n".join(f"- {name}" for name in EMBER_SPECIMENS.values())
    return {
        ("hearth", "hearthbinding"): {
            "name": "Hearthbinding", "x": 0, "y": 0, "icon": "minecraft:campfire", "condition": None,
            "description": "Ember: research",
            "pages": [
                ("text", "Hearthbinding",
                 "Once you understand First Light, look at fire as the Hearthbinders do: something kept and used, "
                 "never let loose. **Sneak and use** one of these to examine it:\\\n" + specimens),
                ("text", "Observed",
                 f"You have examined {OBSERVED_DISTINCT} different things that hold fire. Study one at a "
                 f"**Lampwright's Bench**, or read someone's **Research Notes**.", (research, "observed")),
                ("text", "Understood",
                 "You can now cast **Hearthspark**, **Hearthguard** and **Cinderbolt** with any Concordance "
                 f"instrument. To **master** Hearthbinding, kindle {HEARTH_MASTERY} different kinds of hearth with "
                 f"Hearthspark: {_hearth_list()}.", (research, "understood")),
                ("text", "Mastered",
                 "You can now cast **Hearthflare**, and your Ember invocations cost a point less Focus.",
                 (research, "mastered")),
            ],
        },
        ("hearth", "smoulder"): {
            "name": "Smoulder", "x": 2, "y": 0, "icon": "minecraft:fire_charge", "condition": (research, "understood"),
            "description": "Fire that stays with its target",
            "pages": [
                ("text", "Smoulder",
                 "A smouldering creature burns as anything on fire does, a point of damage a second, while the status "
                 "lasts. Water and rain put it out; Fire Resistance and creatures born of fire ignore it; milk cures "
                 "it. It never sets blocks alight."),
                ("text", "Kindling",
                 "Hearthspark lights a hearth as flint and steel would, and only where you could light it by hand: "
                 "claims, protected towns and spawn protection refuse it. It never places fire."),
            ],
        },
        ("hearth", "regalia"): {
            "name": "A Hearthbinder's Regalia", "x": 4, "y": 0, "icon": rid("fire_focus"), "condition": None,
            "description": "Fire Spell Power, worn",
            "pages": [
                ("text", "A Hearthbinder's Regalia",
                 "The Hearthbinders' gear gives **fire Spell Power**. Cinderbolt and Hearthflare add half a point of "
                 "damage for each whole point of it: two points make one more. Wear a **focus** in the Spell Focus "
                 "slot and a fire set as armour; the most is "
                 f"+{FOCUS_POWER + 4 * ROBE_PIECE_POWER:g}, so Cinderbolt deals 6 and Hearthflare 7. The **Fire "
                 "Bangle** goes in a Bracelet slot. None of it holds Focus: Focus is yours alone."),
                ("crafting_recipe", "Lesser Focus of Fire",
                 f"+{LESSER_FOCUS_POWER:g} fire Spell Power while worn in the Spell Focus slot. One focus at a time.",
                 rid("lesser_fire_focus")),
                ("crafting_recipe", "Focus of Fire",
                 f"The lesser focus set with diamonds: +{FOCUS_POWER:g} fire Spell Power in the Spell Focus slot.",
                 rid("fire_focus")),
                ("crafting_recipe", "Fire Bangle",
                 "Once you understand Hearthbinding, your melee blows leave the creature smouldering for "
                 f"{BANGLE_SMOULDER_TICKS // 20} seconds, wherever you may harm it. It gives no Spell Power, and a "
                 "second bangle adds nothing.", rid("fire_bangle")),
                ("text", "Two Fire Sets",
                 "Each piece of either set gives half a point of fire Spell Power, so only four pieces together raise "
                 "fire damage, by one. The **Pyromaniac's** set is cloth: a little less protection than leather. The "
                 "**Pyromancer's** set, wool and gold, has leather's protection and lasts longer. Spell Power's own "
                 "enchantments cannot be put on either."),
                *[("crafting_recipe", ITEMS[item]["name"], f"{ARMOR_SETS[SET_OF[item]]['name']} set: "
                   f"{sum(ARMOR_SETS[SET_OF[item]]['defense'].values())} protection for the whole set.", rid(item))
                  for item in ARMOR],
            ],
        },
    }


CATEGORY = {"hearth": {"name": "Hearth", "icon": "minecraft:campfire", "sort": 16,
                       "description": "The Hearthbinders' practice: fire kept and used"}}


def tags(tags):
    for ref in EMBER_SPECIMENS:
        tags.add("item", EMBER_SPECIMEN_TAG, ref)


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    # The regalia: the owner's item models (their texture renamed to the Jugcraft copy) and the item definitions.
    for item in ITEMS:
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
        write(assets / "models" / "item" / f"{item}.json", owner_model(item))
    for item, recipe in RECIPES.items():
        write(data / "recipe" / f"{item}.json", {
            "fabric:load_conditions": condition("concordance"), "type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": recipe["pattern"], "key": recipe["key"], "result": {"id": rid(item), "count": 1}})
    for armour_set in ARMOR_SETS:
        write(data / "tags" / "item" / f"{repair_tag(armour_set).split(':')[1]}.json", {"values": ["#minecraft:wool"]})
    # The owner's two slots as Trinkets slots (their icons are the owner's, tools/owner_art.py), the items each takes, and
    # players' having them.
    trinkets = data.parent / "trinkets"
    for slot, info in TRINKET_SLOTS.items():
        group, name = slot.split("/")
        write(trinkets / "slots" / group / f"{name}.json", {"icon": rid(f"container/slots/{info['icon']}"),
                                                            "amount": info["amount"], "validator_predicates": ["trinkets:default"]})
        write(trinkets / "tags" / "item" / group / f"{name}.json",
              {"replace": False, "values": [rid(item) for item, worn in TRINKETS.items() if worn == slot]})
    write(trinkets / "entities" / f"{MOD}_ember.json", {"entities": ["player"], "slots": sorted(TRINKET_SLOTS)})


def write_data(write, data):
    pass
