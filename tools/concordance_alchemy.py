"""Roadmap step 13: experimental alchemy (docs/features/arcane-concordance-alchemy.md).

The Alembists' practice. Ingredients carry amounts of six properties (a property vector); a preparation is an explicit
transformation of that vector; a mixture in an Alembic Crucible tracks its volume (parts of water), the properties
still pending and those dissolved, its contaminant, the temperature band and its whole history. Heat decides how much a
stir dissolves (and searing heat damages delicate properties); bottling draws one part's dose. The rules are data
(data/jugcraft/concordance/ingredient, preparation and property, read by Java's concordance/alchemy); the simulation is
whole milli-units with floor division, so the same process always gives the same result.

tools/concordance.py merges these tables into its own. Numbers the Java repeats are checked by tools/check_mod_data.py.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# The process (Java: alchemy/Mixture.java, Band.java, Heat.java).
MAX_PARTS = 6
MAX_OPERATIONS = 32
SEARING_KEEP = 850  # per thousand of dissolved Radiance and Verdance a searing stir keeps
SEARING_CONTAMINANT = 500  # milli-units of contaminant a searing stir adds
BANDS = {"cold": (None, 100), "warm": (40, 300), "hot": (90, 550), "searing": (160, 800)}  # (from, dissolve per 1000)
AMBIENT = 20
HEAT_RATE = 1  # degrees a tick
HEAT_SOURCES = {"minecraft:magma_block": 60, "minecraft:soul_campfire": 100, "minecraft:campfire": 120,
                "minecraft:soul_fire": 150, "minecraft:fire": 175, "minecraft:lava": 220}
# The crucible (Java: CrucibleBlockEntity.java).
BUFFER_SLOTS = 5  # ingredients waiting for automation
STIR_TICKS = 10  # the most often a crucible can be stirred
AUTOMATION_TICKS = 20  # how often a crucible running a formula takes its next step
WATER_PER_BUCKET = 3  # parts a water bucket pours
# Sampling levels (Java: alchemy/Assay.java).
SPOON, GLASS, MASTERED = 1, 2, 3
ALCHEMY_MASTERY = 3  # distinct outcomes bottled

ALCHEMY_SPECIMEN_TAG = f"{MOD}:alchemy_specimens"
ALCHEMY_SPECIMENS = {
    "minecraft:sugar": "Sugar", "minecraft:sweet_berries": "Sweet Berries", "minecraft:dried_kelp": "Dried Kelp",
    "minecraft:spider_eye": "Spider Eye", "minecraft:bone_meal": "Bone Meal", "minecraft:honeycomb": "Honeycomb",
}

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "alembic_arts": {
        "name": "Alembic Arts",
        "principle": "tide",
        "tradition": "alembists",
        "stage": "practitioner",
        "icon": "minecraft:sugar",
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{ALCHEMY_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{ALCHEMY_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{ALCHEMY_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: different outcomes bottled.
            "mastered": [{"type": "practice", "activity": rid("alchemy"), "distinct": ALCHEMY_MASTERY}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Something in the Mix", "Examine an alchemical ingredient", "task", "minecraft:sugar"),
            "observed": ("A Matter of Taste", "Examine three different alchemical ingredients", "task",
                         "minecraft:sweet_berries"),
            "understood": ("Alembic Arts", "Understand alchemy: study an ingredient at a Lampwright's Bench", "goal",
                           rid("crucible")),
            "mastered": ("Alembist", f"Bottle {ALCHEMY_MASTERY} different outcomes", "challenge", rid("assay_glass")),
        },
        "locked": {"encountered": "Examine an alchemical ingredient first",
                   "observed": "Examine three different ingredients first",
                   "understood": "Understand the Alembic Arts first", "mastered": "Master the Alembic Arts first"},
    },
}

# ------------------------------------------------------------------------------------------------- the rules

AXES = ["radiance", "verdance", "ember", "rime", "tide", "hollow"]  # Java: alchemy/Axis.java, in order
MAX_PROPERTY = 10.0  # Java: RulesParser.MAX_PROPERTY

# Units of each property an ingredient carries, and the contaminant it brings.
INGREDIENTS = {
    "glowstone_dust": {"item": "minecraft:glowstone_dust", "properties": {"radiance": 1.5}},
    "glow_berries": {"item": "minecraft:glow_berries", "properties": {"radiance": 0.6, "verdance": 0.6}},
    "sweet_berries": {"item": "minecraft:sweet_berries", "properties": {"verdance": 1.0}},
    "bone_meal": {"item": "minecraft:bone_meal", "properties": {"verdance": 0.8, "hollow": 0.3}, "contaminant": 0.2},
    "sugar": {"item": "minecraft:sugar", "properties": {"ember": 0.9, "tide": 0.2}},
    "magma_cream": {"item": "minecraft:magma_cream", "properties": {"ember": 1.6}, "contaminant": 0.2},
    "snowball": {"item": "minecraft:snowball", "properties": {"rime": 1.0}},
    "amethyst_shard": {"item": "minecraft:amethyst_shard", "properties": {"rime": 0.8, "radiance": 0.4}},
    "dried_kelp": {"item": "minecraft:dried_kelp", "properties": {"tide": 1.2}},
    "honeycomb": {"item": "minecraft:honeycomb", "properties": {"tide": 0.6, "verdance": 0.4}},
    "spider_eye": {"item": "minecraft:spider_eye", "properties": {"hollow": 1.2}, "contaminant": 0.3},
    # The Greenwardens' crops (roadmap step 14, tools/concordance_ecology.py): a garden's harvest for the crucible.
    "sunpetal": {"item": "jugcraft:sunpetal", "properties": {"radiance": 1.0, "verdance": 0.6}},
    "dewmoss": {"item": "jugcraft:dewmoss", "properties": {"tide": 1.4, "verdance": 0.3}},
    "gloamcap": {"item": "jugcraft:gloamcap", "properties": {"hollow": 0.9, "rime": 0.4}, "contaminant": 0.1},
    "mendvetch": {"item": "jugcraft:mendvetch", "properties": {"verdance": 1.4}},
}
# How an ingredient is prepared: scaled, part dissolved at once ("ready"), the rest stirred out.
PREPARATIONS = {
    "raw": {"scale": 1.0, "ready": 0.0},
    # Ground in a mortar: a little lost as dust, but much of it dissolves the moment it goes in.
    "ground": {"tool": rid("mortar"), "scale": 0.9, "ready": 0.6},
}
# What a dose a part does: at the threshold, level I; a level more each per_level; ticks_per_unit for each unit.
PROPERTIES = {
    "radiance": {"status": "minecraft:night_vision", "intent": "helpful", "threshold": 0.5, "per_level": 10.0,
                 "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
    # Regeneration and Poison reach level II, so they last no longer than vanilla's strong potions (450 and 432 ticks).
    "verdance": {"status": "minecraft:regeneration", "intent": "helpful", "threshold": 0.6, "per_level": 1.5,
                 "max_level": 2, "ticks_per_unit": 300, "max_ticks": 450},
    "ember": {"status": "minecraft:fire_resistance", "intent": "helpful", "threshold": 0.6, "per_level": 10.0,
              "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
    "rime": {"status": "minecraft:resistance", "intent": "helpful", "threshold": 0.8, "per_level": 10.0,
             "max_level": 1, "ticks_per_unit": 600, "max_ticks": 1200},
    "tide": {"status": "minecraft:water_breathing", "intent": "helpful", "threshold": 0.6, "per_level": 10.0,
             "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
    "hollow": {"status": "minecraft:poison", "intent": "harmful", "threshold": 0.4, "per_level": 1.0,
               "max_level": 2, "ticks_per_unit": 200, "max_ticks": 432},
    "contaminant": {"status": "minecraft:nausea", "intent": "harmful", "threshold": 0.5, "per_level": 10.0,
                    "max_level": 1, "ticks_per_unit": 160, "max_ticks": 400},
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "mortar": {"name": "Mortar and Pestle",
               "tooltip": "Hold an ingredient in your other hand and use this to grind one: ground, it dissolves "
                          "faster but loses a little. A Dried Chili grinds into Paprika."},
    "stirring_rod": {"name": "Stirring Rod", "tooltip": "Use it on an Alembic Crucible to stir the mixture once."},
    "sampling_spoon": {"name": "Sampling Spoon",
                       "tooltip": "Use it on an Alembic Crucible: how hot, how much, and what it tastes of."},
    "assay_glass": {"name": "Assay Glass",
                    "tooltip": "Use it on an Alembic Crucible to measure every property, pending and dissolved. In a "
                               "master's hands it also says what a dose would do, and why."},
    "formula": {"name": "Formula",
                "tooltip": "Use a blank formula on a crucible to record its process; sneak to set a written one for "
                           "the crucible to repeat."},
    "reagent": {"name": "Reagent", "tooltip": "A prepared ingredient for the crucible."},
    "draught": {"name": "Draught", "tooltip": "Drink it."},
    "salve": {"name": "Salve", "tooltip": "Use it on a creature to apply it (or on yourself, sneaking)."},
}
BLOCKS = {"crucible": {"name": "Alembic Crucible"}}

# ------------------------------------------------------------------------------------------------- text

MESSAGES = {
    "alchemy.unknown": "You have not learned the Alembic Arts (the codex says how)",
    "alchemy.added": "Added %s",
    "alchemy.stirred": "Stirred (%s)",
    "alchemy.water": "Poured in water: %s parts",
    "alchemy.bottled": "Bottled a %s",
    "alchemy.nothing_to_bottle": "The crucible is empty",
    "alchemy.no_formula": "Nothing can be recorded: the crucible must hold a mixture nothing has been drawn from",
    "alchemy.recorded": "Formula recorded: %s steps",
    "alchemy.formula_set": "The crucible will repeat this formula (%s steps)",
    "alchemy.formula_cleared": "The crucible no longer repeats a formula",
    "alchemy.formula_invalid": "This formula cannot be followed here",
    "alchemy.automation_busy": "The crucible is following a formula",
    "alchemy.ground": "Ground a %s",
    "alchemy.cannot_grind": "Hold an alchemical ingredient in your other hand to grind it",
    "alchemy.settling": "Let the mixture settle a moment",
    "alchemy.applied": "Applied the salve",
    "alchemy.no_effect": "It does nothing",
    "crucible.status": "Alembic Crucible: %s, %s degrees, %s parts",
    "examine.substance": "You taste a pinch of the %s and note what it is made of",
}

# The simulation's own words (compose.jugcraft.alchemy.*, shown through ComposeText like the compiler's).
TEXT = {
    "alchemy.too_long": "A process has at most %s steps",
    "alchemy.overflowing": "The crucible holds at most %s parts",
    "alchemy.not_ingredient": "%s is not an alchemical ingredient",
    "alchemy.no_water": "Pour in water first",
    "alchemy.reason": "%s %s for %s s: %s %s a part (needs %s) from %s",
    "alchemy.too_weak": "%s too weak: %s a part (needs %s) from %s",
    "alchemy.assay.empty": "Empty (%s)",
    "alchemy.assay.state": "%s, %s parts",
    "alchemy.assay.taste": "It tastes most of %s",
    "alchemy.assay.plain": "It tastes of water",
    "alchemy.assay.murky": "It is murky",
    "alchemy.assay.temperature": "%s degrees",
    "alchemy.assay.axis": "%s: %s dissolved, %s pending, a part",
    "alchemy.assay.contaminant": "Contaminant: %s a part",
    "alchemy.assay.nothing": "A dose now would do nothing",
    "band.cold": "Cold", "band.warm": "Warm", "band.hot": "Hot", "band.searing": "Searing",
    "alchemy.contaminant": "Contaminant",
    "alchemy.searing": "searing stirs",
}

TOOLTIPS = {
    "alchemy_specimen": "Alchemical ingredient: sneak and use to examine it",
    "reagent": "Ground %s",
    "brew.effect": "%s %s (%s s)",
    "brew.none": "No effect",
    "formula.blank": "Blank",
    "formula.steps": "%s steps: %s",
    "jade.temperature": "%s degrees (%s)",
    "jade.volume": "%s / %s parts",
    "jade.step": "Formula: step %s of %s",
    "jade.waiting": "Waiting for: %s",
}

SOUND_EVENTS = {
    "concordance.crucible_stir": "Mixture stirred",
    "concordance.crucible_add": "Ingredient added",
    "concordance.crucible_bottle": "Mixture bottled",
}


def lang_entries(lang):
    lang[f"tag.item.{ALCHEMY_SPECIMEN_TAG.replace(':', '.')}"] = "Alchemical Ingredients"
    for key, text in TEXT.items():
        lang[f"compose.{MOD}.{key}"] = text
    lang[f"config.jade.plugin_{MOD}.crucible"] = BLOCKS["crucible"]["name"]
    for key, info in INGREDIENTS.items():
        lang[f"jei.{MOD}.alchemy.{key}"] = "Alchemy: " + ", ".join(
            f"{axis.title()} {value:.2f}" for axis, value in info["properties"].items()) + (
            f"; contaminant {info['contaminant']:.2f}" if info.get("contaminant") else "")
    lang[f"jei.{MOD}.alchemy.mortar"] = "Grind an alchemical ingredient: it dissolves faster but loses a little."


# ------------------------------------------------------------------------------------------------- codex

def _units(value):
    return f"{value:.2f}"


def _ingredient_table():
    return "\\\n".join(f"- {ALCHEMY_SPECIMENS.get(info['item'], info['item'].split(':')[1].replace('_', ' ').title())}: "
                       + ", ".join(f"{axis} {_units(v)}" for axis, v in info["properties"].items())
                       + (f" (contaminant {_units(info['contaminant'])})" if info.get("contaminant") else "")
                       for info in INGREDIENTS.values())


def _seconds(ticks):
    value = ticks / 20
    return str(int(value)) if value == int(value) else f"{value:g}"


def _property_table():
    names = {"minecraft:night_vision": "Night Vision", "minecraft:regeneration": "Regeneration",
             "minecraft:fire_resistance": "Fire Resistance", "minecraft:resistance": "Resistance",
             "minecraft:water_breathing": "Water Breathing", "minecraft:poison": "Poison", "minecraft:nausea": "Nausea"}
    return "\\\n".join(f"- {axis.title()}: {names[info['status']]} from {_units(info['threshold'])} a part, "
                       f"{_seconds(info['ticks_per_unit'])} s a unit (at most {_seconds(info['max_ticks'])} s)"
                       for axis, info in PROPERTIES.items())


def codex():
    art = "alembic_arts"
    specimens = "\\\n".join(f"- {name}" for name in ALCHEMY_SPECIMENS.values())
    bands = ", ".join(f"**{name.title()}** {('below ' + str(BANDS['warm'][0])) if name == 'cold' else ('from ' + str(lo))} "
                      f"({dissolve // 10}% a stir)" for name, (lo, dissolve) in BANDS.items())
    return {
        ("alembic", "alembic_arts"): {
            "name": "Alembic Arts", "x": 0, "y": 0, "icon": "minecraft:sugar", "condition": None,
            "description": "Alchemy: research",
            "pages": [
                ("text", "Alembic Arts",
                 "Once you understand First Light, look closely at what things are made of. **Sneak and use** one of "
                 "these to examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 f"You can now work an **Alembic Crucible**. To **master** the Alembic Arts, bottle "
                 f"{ALCHEMY_MASTERY} different outcomes.", (art, "understood")),
                ("text", "Mastered", "Your Assay Glass now tells you what a dose would do, and why.", (art, "mastered")),
            ],
        },
        ("alembic", "crucible"): {
            "name": "Alembic Crucible", "x": 2, "y": 0, "icon": rid("crucible"), "condition": (art, "encountered"),
            "description": "Water, heat and patience",
            "pages": [
                ("crafting_recipe", "Alembic Crucible",
                 f"Pour in water (a water bucket is {WATER_PER_BUCKET} parts; at most {MAX_PARTS}), then use "
                 f"ingredients on it. Bottle with a glass bottle (a draught) or a bowl (a salve).", rid("crucible")),
                ("text", "Heat",
                 f"The crucible takes the heat of what is beneath it: magma {HEAT_SOURCES['minecraft:magma_block']}, "
                 f"a campfire {HEAT_SOURCES['minecraft:campfire']}, fire {HEAT_SOURCES['minecraft:fire']}, lava "
                 f"{HEAT_SOURCES['minecraft:lava']} degrees (nothing: {AMBIENT}). It warms or cools one degree a tick."
                 f"\\\n\\\n{bands}."),
                ("text", "Stirring",
                 f"Ingredients do not dissolve by themselves. Each **stir** (a Stirring Rod or a stick) dissolves a "
                 f"share of what is still pending, more the hotter it is. But a **searing** stir keeps only "
                 f"{SEARING_KEEP // 10}% of the dissolved Radiance and Verdance and fouls the mixture."),
                ("text", "Bottling",
                 "A bottle takes one part's share of everything dissolved: that is its dose. The same process always "
                 "makes the same mixture, so a good one can be made again."),
            ],
        },
        ("alembic", "properties"): {
            "name": "Properties", "x": 4, "y": 0, "icon": "minecraft:glowstone_dust", "condition": (art, "observed"),
            "description": "What ingredients carry, and what it does",
            "pages": [
                ("text", "Ingredients", _ingredient_table()),
                ("text", "What a Dose Does", _property_table() + "\\\n\\\nA dose below a threshold does nothing for "
                                                                 "that property; too much contaminant brings nausea."),
                ("text", "Preparation",
                 "Used as it comes, an ingredient waits to be stirred out. **Ground** in a Mortar and Pestle it loses a "
                 "tenth, but most of it dissolves the moment it goes in."),
            ],
        },
        ("alembic", "sampling"): {
            "name": "Sampling", "x": 0, "y": 2, "icon": rid("assay_glass"), "condition": (art, "encountered"),
            "description": "Knowing what is in the pot",
            "pages": [
                ("crafting_recipe", "Sampling Spoon",
                 "Tells you how hot the mixture is, how much there is, which property is strongest and whether it is "
                 "murky.", rid("sampling_spoon")),
                ("crafting_recipe", "Assay Glass",
                 "Measures every property, dissolved and pending, and the contaminant, a part at a time. Once you have "
                 "mastered the Alembic Arts it also says what a dose would do and why: which property, how strong "
                 "against its threshold, and which ingredients put it there.", rid("assay_glass")),
            ],
        },
        ("alembic", "formulas"): {
            "name": "Formulas", "x": 2, "y": 2, "icon": rid("formula"), "condition": (art, "understood"),
            "description": "Recording a process, and repeating it",
            "pages": [
                ("crafting_recipe", "Formula",
                 "Use a blank formula on a crucible to write down its whole process (water, every ingredient and how "
                 "it was prepared, every stir and how hot) while nothing has been drawn from it.", rid("formula")),
                ("text", "Repeating It",
                 f"Use a written formula on a crucible to set it (**sneak** with an empty hand to clear it). Fed "
                 f"water through a pipe and "
                 f"ingredients through a hopper (up to {BUFFER_SLOTS} kinds waiting), it follows the formula step "
                 f"by step, waiting for the heat each stir needs, and bottles into its output when it has bottles. "
                 f"A Pneumatic Extractor at its side takes the brews out, so the fire can stay beneath. "
                 f"It never guesses: a missing ingredient or the wrong heat simply waits."),
            ],
        },
    }


CATEGORY = {"alembic": {"name": "Alembic", "icon": "minecraft:sugar", "sort": 5,
                        "description": "The Alembists' practice: experimental alchemy"}}


# ------------------------------------------------------------------------------------------------- data and assets

def ingredient_json(info):
    out = {"schema": 1, "item": info["item"], "properties": info["properties"]}
    if info.get("contaminant"):
        out["contaminant"] = info["contaminant"]
    return out


def preparation_json(info):
    return dict({"schema": 1}, **info)


def property_json(info):
    return dict({"schema": 1}, **info)


def tags(tags):
    for ref in ALCHEMY_SPECIMENS:
        tags.add("item", ALCHEMY_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("crucible"))


def crucible_geo():
    """The Alembic Crucible for GeckoLib: a squat copper-banded iron pot on three feet, the liquid the animations raise
    and settle, and a long brass ladle resting in it. Box UVs into a 64x64 frame laid out by CRUCIBLE_REGIONS (the
    texture is an animated strip of such frames; tools/concordance_alchemy_art.py paints the same regions)."""
    def cube(name, origin, size):
        u, v = CRUCIBLE_UV[name]
        return {"origin": origin, "size": size, "uv": [u, v]}
    bones = [
        {"name": "pot", "pivot": [0, 0, 0], "cubes": [
            cube("floor", [-6, 2, -6], [12, 1, 12]),
            cube("wall_ns", [-6, 3, -6], [12, 8, 1]),
            cube("wall_ns", [-6, 3, 5], [12, 8, 1]),
            cube("wall_ew", [-6, 3, -5], [1, 8, 10]),
            cube("wall_ew", [5, 3, -5], [1, 8, 10]),
            cube("band", [-6.5, 8, -6.5], [13, 1, 13]),
        ]},
        {"name": "feet", "parent": "pot", "pivot": [0, 0, 0], "cubes": [
            cube("foot", [-5, 0, -5], [2, 2, 2]), cube("foot", [3, 0, -5], [2, 2, 2]), cube("foot", [-1, 0, 3], [2, 2, 2]),
        ]},
        {"name": "liquid", "parent": "pot", "pivot": [0, 3, 0], "cubes": [cube("liquid", [-5, 3, -5], [10, 6, 10])]},
        {"name": "ladle", "parent": "pot", "pivot": [2.5, 9, 0], "cubes": [
            cube("shaft", [2, 5, -0.5], [1, 12, 1]), cube("scoop", [1, 4, -1.5], [3, 1, 3]),
        ]},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.crucible", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.75, 0]},
        "bones": bones}]}


# Box-UV origins in the crucible's 64x64 frame; each cube of size (w, h, d) uses (2(w + d)) x (d + h) from there.
CRUCIBLE_UV = {"floor": (0, 0), "foot": (48, 0), "scoop": (48, 4), "shaft": (60, 0), "band": (0, 13),
               "wall_ns": (0, 27), "wall_ew": (26, 27), "liquid": (0, 45)}
CRUCIBLE_SIZES = {"floor": (12, 1, 12), "foot": (2, 2, 2), "scoop": (3, 1, 3), "shaft": (1, 12, 1), "band": (13, 1, 13),
                  "wall_ns": (12, 8, 1), "wall_ew": (1, 8, 10), "liquid": (10, 6, 10)}
CRUCIBLE_FRAMES = 4  # the liquid ripples through these frames (textures/block/crucible.png.mcmeta)


def crucible_animations():
    """Empty: no liquid. Still: the liquid sits flat. Simmer: it heaves gently and the ladle sways."""
    return {"format_version": "1.8.0", "animations": {
        "animation.crucible.empty": {"loop": True, "animation_length": 1.0, "bones": {
            "liquid": {"scale": {"0.0": [1, 0, 1]}}}},
        "animation.crucible.still": {"loop": True, "animation_length": 1.0, "bones": {
            "liquid": {"scale": {"0.0": [1, 1, 1]}}}},
        "animation.crucible.simmer": {"loop": True, "animation_length": 2.0, "bones": {
            "liquid": {"scale": {"0.0": [1, 1, 1], "1.0": [1, 1.06, 1], "2.0": [1, 1, 1]}},
            "ladle": {"rotation": {"0.0": [0, 0, -6], "1.0": [0, 0, 6], "2.0": [0, 0, -6]}}}},
    }}


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    for item in ("mortar", "stirring_rod", "sampling_spoon", "assay_glass", "reagent", "draught", "salve"):
        parent = "minecraft:item/handheld" if item in ("stirring_rod", "sampling_spoon") else "minecraft:item/generated"
        write(assets / "models" / "item" / f"{item}.json", {"parent": parent, "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    # A formula looks blank until something is written on it.
    for model in ("formula", "formula_written"):
        write(assets / "models" / "item" / f"{model}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{model}")}})
    write(assets / "items" / "formula.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component", "component": rid("formula"),
        "on_true": {"type": "minecraft:model", "model": rid("item/formula_written")},
        "on_false": {"type": "minecraft:model", "model": rid("item/formula")}}})
    # The crucible: GeckoLib draws it (its block model is only the breaking particles).
    write(assets / "geckolib" / "models" / "block" / "crucible.geo.json", crucible_geo())
    write(assets / "geckolib" / "animations" / "block" / "crucible.animation.json", crucible_animations())
    write(assets / "textures" / "block" / "crucible.png.mcmeta", {"animation": {"frametime": 4}})
    write(assets / "models" / "block" / "crucible.json", {"textures": {"particle": "minecraft:block/iron_block"}})
    write(assets / "blockstates" / "crucible.json", {"variants": {"": {"model": rid("block/crucible")}}})
    write(assets / "models" / "item" / "crucible.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/crucible")}})
    write(assets / "items" / "crucible.json", {"model": {"type": "minecraft:model", "model": rid("item/crucible")}})
    write(data / "loot_table" / "blocks" / "crucible.json", self_drop("crucible"))
    feature = "concordance"
    recipes = {
        "crucible": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["C C", "CKC", "BBB"],
                     "key": {"C": "minecraft:copper_ingot", "K": "minecraft:cauldron", "B": "minecraft:bricks"},
                     "result": {"id": rid("crucible"), "count": 1}},
        "mortar": {"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": ["minecraft:bowl", "minecraft:flint", "minecraft:stone"],
                   "result": {"id": rid("mortar"), "count": 1}},
        "stirring_rod": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["  C", " S ", "S  "],
                         "key": {"C": "minecraft:copper_ingot", "S": "minecraft:stick"},
                         "result": {"id": rid("stirring_rod"), "count": 1}},
        "sampling_spoon": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["C", "S"],
                           "key": {"C": "minecraft:copper_ingot", "S": "minecraft:stick"},
                           "result": {"id": rid("sampling_spoon"), "count": 1}},
        "assay_glass": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" G ", "GAG", " C "],
                        "key": {"G": "minecraft:glass_pane", "A": "minecraft:amethyst_shard", "C": "minecraft:copper_ingot"},
                        "result": {"id": rid("assay_glass"), "count": 1}},
        "formula": {"type": "minecraft:crafting_shapeless", "category": "misc",
                    "ingredients": ["minecraft:paper", "minecraft:ink_sac", "minecraft:glass_bottle"],
                    "result": {"id": rid("formula"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key, info in INGREDIENTS.items():
        write(data / "concordance" / "ingredient" / f"{key}.json", ingredient_json(info))
    for key, info in PREPARATIONS.items():
        write(data / "concordance" / "preparation" / f"{key}.json", preparation_json(info))
    for key, info in PROPERTIES.items():
        write(data / "concordance" / "property" / f"{key}.json", property_json(info))


def recipe_ingredients():
    """For JEI (client/compat/JugcraftJeiPlugin "alchemy_ingredients"): what each ingredient carries, public data;
    never a formula or an outcome."""
    return [{"item": info["item"], "key": key} for key, info in INGREDIENTS.items()]
