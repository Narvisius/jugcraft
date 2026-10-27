"""Roadmap step 13: experimental alchemy (docs/features/arcane-concordance-alchemy.md).

The Alembists' practice. Ingredients carry amounts of six properties (a property vector); a preparation is an explicit
transformation of that vector; a mixture in an Alembic Crucible tracks its volume (parts of water), the properties
still pending and those dissolved, its contaminant, the temperature band and its whole history. Heat decides how much a
stir dissolves (searing heat damages delicate properties, and any heat above cold thaws Rime: Concordance Chunk B, B-08);
bottling draws one part's dose. The rules are data
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
# Rime is cold's own (plan 03:36, "too much heat consumes the temporary cold property"; Concordance Chunk B, B-08, B-P18):
# per thousand of the Rime already dissolved that one stir at each band keeps (Java: Mixture.rimeKeep, RIME_KEEP_*).
# Cold keeps it all; no hotter band may keep more than a cooler one (check_mod_data.py check_winter_brine).
RIME_KEEP = {"cold": 1000, "warm": 600, "hot": 300, "searing": 0}
AMBIENT = 20
HEAT_RATE = 1  # degrees a tick
HEAT_SOURCES = {"minecraft:magma_block": 60, "minecraft:soul_campfire": 100, "minecraft:campfire": 120,
                "minecraft:soul_fire": 150, "minecraft:fire": 175, "minecraft:lava": 220}
# Concordance Chunk C, C-05 (C-P48): the Crucible Stand's art, each a byte-for-byte copy of the owner's file (copied and
# checked by tools/owner_art.py block_asset_imports): treated iron for its cladding and frame (posts and deck), and a
# copper coil seen from its side (the coil's ends lie under the deck and on the cladding, so its end-on face is never
# seen and is not imported). Searched first: art/owner-library/catalog/files.csv.
OWNER_BLOCKS_FILES = {
    "textures/block/crucible_stand_plates.png": "Guns/block/treated_iron_plates.png",
    "textures/block/crucible_stand_frame.png": "Guns/block/treated_iron_block.png",
    "textures/block/crucible_stand_coil.png": "Big Cannons and Mounted Guns/textures/block/coilside.png",
}
# Concordance Chunk C, C-05 (C-D3, C-P12; starting values): where a Crucible Stand heats the crucible on it for a stir at
# each band (cold is off: the crucible settles at AMBIENT), the JE a tick it pays while it heats, its store, what it takes
# a tick from a cable, and how often one player may set it (Java: alchemy/Heat.STAND_*, CrucibleStandBlockEntity). Each
# target lies inside its band; a hotter band costs more (check_crucible_stand). It is never a HEAT_SOURCES block.
STAND_TARGETS = {"warm": 65, "hot": 120, "searing": 175}
STAND_JE = {"warm": 8, "hot": 16, "searing": 32}
STAND_CAPACITY = 640
STAND_INTAKE = 64
STAND_SET_TICKS = 10
# Its power sockets: every face that takes JE, never its seat (tools/machine_connections.py with_model_sockets; C-P44).
STAND_PORTS = ("north", "south", "west", "east", "down")
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
# A preparation in a step's words (Java: Operation.ingredient; ComposeText names "preparation" refs preparation.<ns>.<path>).
PREPARATION_NAMES = {"raw": "as it comes", "ground": "ground"}
# Winter Brine's status (Concordance Chunk B, B-08, B-D3; Java: concordance/rime/Frostward.STATUS): no frost gathers while
# it lasts and the Yeti King's chill does not take. tools/concordance_rime.py holds its colour, name and icon.
FROSTWARD = rid("frostward")
# What a dose a part does: at the threshold, level I; a level more each per_level; ticks_per_unit for each unit.
PROPERTIES = {
    "radiance": {"status": "minecraft:night_vision", "intent": "helpful", "threshold": 0.5, "per_level": 10.0,
                 "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
    # Regeneration and Poison reach level II, so they last no longer than vanilla's strong potions (450 and 432 ticks).
    # Concordance Chunk C, C-02 (C-P7): each unit of contaminant a part takes this many units of Verdance a part before
    # the dose is judged (Java: Property.fouled, in Outcome.of). A starting value.
    "verdance": {"status": "minecraft:regeneration", "intent": "helpful", "threshold": 0.6, "per_level": 1.5,
                 "max_level": 2, "ticks_per_unit": 300, "max_ticks": 450, "fouling": 4.0},
    "ember": {"status": "minecraft:fire_resistance", "intent": "helpful", "threshold": 0.6, "per_level": 10.0,
              "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
    # B-08: Rime brews Frostward (no salt: a snowball carries 1.0, B-P17), timed as the Fire Resistance brew is (B-P19). A
    # bottle keeps the status it was bottled with (concordance/Brew.Dose), so Rime brews made before still give Resistance.
    "rime": {"status": FROSTWARD, "intent": "helpful", "threshold": 0.8, "per_level": 10.0,
             "max_level": 1, "ticks_per_unit": 1200, "max_ticks": 2400},
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
                "tooltip": "Use a blank formula on a crucible to record its process; use a written one to set it for "
                           "the crucible to repeat (sneak with an empty hand to clear it)."},
    "reagent": {"name": "Reagent", "tooltip": "A prepared ingredient for the crucible."},
    "draught": {"name": "Draught", "tooltip": "Drink it."},
    "salve": {"name": "Salve", "tooltip": "Use it on a creature to apply it (or on yourself, sneaking)."},
}
BLOCKS = {"crucible": {"name": "Alembic Crucible"}, "crucible_stand": {"name": "Crucible Stand"}}

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
    "alchemy.formula_invalid": "This formula can no longer be read",
    "alchemy.formula_refused": "Not set: %s",
    "alchemy.formula_set_risk": "The crucible will repeat this formula (%s steps). %s",
    "alchemy.automation_busy": "The crucible is following a formula",
    "alchemy.ground": "Ground a %s",
    "alchemy.cannot_grind": "Hold an alchemical ingredient in your other hand to grind it",
    "alchemy.settling": "Let the mixture settle a moment",
    "alchemy.applied": "Applied the salve",
    "alchemy.no_effect": "It does nothing",
    "crucible.status": "Alembic Crucible: %s, %s degrees, %s parts",
    "crucible.status_waiting": "%s. %s",
    # Concordance Chunk C, C-05: the Crucible Stand's empty-hand words.
    "stand.status": "Crucible Stand: %s; %s of %s JE",
    "stand.status_more": "%s. %s",
    "stand.following": "following its crucible's formula, %s",
    "stand.setting": "set to %s",
    "stand.heat": "%s (%s degrees)",
    "stand.off": "off",
    "stand.heating": "Heating: %s JE a tick",
    "stand.set": "Crucible Stand set to %s",
    "stand.settling": "Let the stand settle a moment",
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
    "alchemy.thawed": "stirs above the cold, which thaw Rime",
    # Concordance Chunk C, C-01: a formula's step in the player's words (Operation.describe), and what is never hidden
    # (Assay.risks).
    "alchemy.op.water": "%s parts of water",
    "alchemy.op.add": "%s, %s",
    "alchemy.op.stir": "a %s stir",
    "alchemy.risk.harm": "Danger: a dose would bring %s",
    # A searing stir fouls the mixture, keeps SEARING_KEEP of its Radiance and Verdance and none of its Rime (RIME_KEEP).
    "alchemy.risk.searing": "Danger: step %s stirs it searing, which fouls it, spoils its Radiance and Verdance and thaws all its Rime",
    "alchemy.risk.seared": "Danger: it was stirred searing, which fouled it, spoiled its Radiance and Verdance and thawed all its Rime",
    # Concordance Chunk C, C-01: the encyclopedia's Inspect page on a crucible (alchemy/CrucibleInspection).
    "alchemy.inspect.heat": "%s degrees (%s)",
    "alchemy.inspect.heated": "%s beneath heats it towards %s degrees",
    "alchemy.inspect.unlit": "%s beneath is not lit: it settles towards %s degrees",
    "alchemy.inspect.unheated": "%s beneath does not heat a crucible: it settles towards %s degrees",
    "alchemy.inspect.held": "Preserve holds it in the %s band for %s s more",
    "alchemy.inspect.volume": "%s of %s parts",
    "alchemy.inspect.tank": "Its tank holds %s of %s parts of water",
    "alchemy.inspect.no_formula": "It follows no formula",
    "alchemy.inspect.step": "Formula: step %s of %s: %s",
    "alchemy.inspect.step.bottle": "Formula: all %s steps taken; it bottles what it made",
    "alchemy.inspect.sample.spoon": "What a spoon finds",
    "alchemy.inspect.sample.glass": "What your Assay Glass finds",
    "alchemy.inspect.sample.mastered": "What your Assay Glass finds, and what a dose would do",
    "alchemy.inspect.measures": "Measured with the glass",
    "alchemy.inspect.thresholds": "Contaminant %s a part: murky from %s, Nausea from %s",
    "alchemy.inspect.operations": "%s of %s steps taken since it was empty",
    "alchemy.inspect.drawn": "%s doses drawn: its process can no longer be recorded",
    "alchemy.inspect.preview": "The formula, repeated now",
    # Concordance Chunk C, C-02 (C-P7): what the contaminant takes from a property that declares fouling (Outcome.of, and
    # the Assay Glass's line).
    "alchemy.fouled": "%s fouled: %s a part lost to %s contaminant a part, from %s",
    "alchemy.assay.fouls": "%s: the contaminant takes %s a part",
    # Concordance Chunk C, C-05: the Crucible Stand in the shared vocabulary's unavailable_power ("%s holds %s; it needs %s").
    "alchemy.stand.it": "The Crucible Stand",
    "alchemy.stand.je": "%s JE",
    "alchemy.stand.je_tick": "%s JE a tick",
}
# What JEI's page for an ingredient carrying Rime adds (Concordance Chunk B, B-08): a property fact, never an outcome.
RIME_JEI = "; stirred cold, its Rime brews Frostward (Winter Brine)"
# What JEI's page for a workshop product's ingredient adds (Concordance Chunk C, C-02): the product its property brews.
PRODUCT_JEI = "; its {axis} brews {status}: in a {container}, the {name}"
CONTAINERS = {"draught": "bottle", "salve": "bowl"}

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
    # Concordance Chunk C, C-01 (C-P4): the next step is still shown to anyone, raw; why it waits is the crucible's reason.
    "jade.waiting": "Next step: %s",
    "jade.tank": "Water: %s / %s parts",
    # Concordance Chunk C, C-05: Jade on a Crucible Stand.
    "jade.stand_energy": "Energy: %s / %s JE",
    "jade.stand_setting": "Set to %s",
    "jade.stand_following": "Following its crucible's formula: %s",
    "jade.stand_heat": "%s, %s degrees",
    "jade.stand_off": "off",
    "jade.stand_draw": "Heating: %s JE a tick",
    "jade.stand_waits": "Idle while its crucible waits:",
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
    lang[f"config.jade.plugin_{MOD}.crucible_stand"] = BLOCKS["crucible_stand"]["name"]
    for key, info in INGREDIENTS.items():
        lang[f"jei.{MOD}.alchemy.{key}"] = "Alchemy: " + ", ".join(
            f"{axis.title()} {value:.2f}" for axis, value in info["properties"].items()) + (
            f"; contaminant {info['contaminant']:.2f}" if info.get("contaminant") else "") + (
            RIME_JEI if info["properties"].get("rime") else "") + "".join(
            PRODUCT_JEI.format(axis=product["property"].title(), status=STATUS_NAMES[PROPERTIES[product["property"]]["status"]],
                               container=CONTAINERS[product["form"]], name=product["name"])
            for product in PRODUCTS.values() if key in product["jei"])
    for key, info in PRODUCTS.items():
        lang[product_name_key(key, info)] = info["name"]
    lang[f"jei.{MOD}.alchemy.mortar"] = "Grind an alchemical ingredient: it dissolves faster but loses a little."
    for key, name in PREPARATION_NAMES.items():
        lang[f"preparation.{MOD}.{key}"] = name


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


def _rime_keeps():
    """What a stir in each band above cold keeps of the dissolved Rime, as "warm keeps 60%, hot 30%, searing none"."""
    kept = [f"{band} {RIME_KEEP[band] // 10}%" if RIME_KEEP[band] else f"{band} none" for band in BANDS if band != "cold"]
    kept[0] = kept[0].replace(" ", " keeps ", 1)
    return ", ".join(kept)


def _milli(units):
    return round(units * 1000)


def simulate(operations):
    """The process as Java's alchemy/Mixture does it, in milli-units with floor division: ("water", n), ("add", key, prep),
    ("stir", band) -> (parts, pending, dissolved, contaminant), each by axis. For the codex's worked Winter Brine and
    check_mod_data.py check_winter_brine, which hold it to ConcordanceAlchemyGameTests."""
    parts, pending, dissolved, contaminant = 0, dict.fromkeys(AXES, 0), dict.fromkeys(AXES, 0), 0
    for operation in operations:
        if operation[0] == "water":
            parts += operation[1]
        elif operation[0] == "add":
            ingredient, preparation = INGREDIENTS[operation[1]], PREPARATIONS[operation[2]]
            for axis in AXES:
                prepared = _milli(ingredient["properties"].get(axis, 0)) * _milli(preparation["scale"]) // 1000
                ready = prepared * _milli(preparation["ready"]) // 1000
                pending[axis] += prepared - ready
                dissolved[axis] += ready
            contaminant += _milli(ingredient.get("contaminant", 0)) + _milli(preparation.get("contaminant", 0))
        else:
            band = operation[1]
            for axis in AXES:
                moved = pending[axis] * BANDS[band][1] // 1000
                pending[axis] -= moved
                dissolved[axis] += moved
            if band == "searing":
                for axis in ("radiance", "verdance"):
                    dissolved[axis] = dissolved[axis] * SEARING_KEEP // 1000
                contaminant += SEARING_CONTAMINANT
            dissolved["rime"] = dissolved["rime"] * RIME_KEEP[band] // 1000
    return parts, pending, dissolved, contaminant


# Winter Brine as the codex and ConcordanceAlchemyGameTests.winterBrineNeedsTheColdBand work it: three snowballs as they
# come in one part of water, then WINTER_BRINE_STIRS stirs at one band.
WINTER_BRINE = [("water", 1)] + [("add", "snowball", "raw")] * 3
WINTER_BRINE_STIRS = 3


def winter_brine(band):
    """Rime a part (milli-units) of WINTER_BRINE stirred WINTER_BRINE_STIRS times at {band}."""
    parts, _, dissolved, _ = simulate(WINTER_BRINE + [("stir", band)] * WINTER_BRINE_STIRS)
    return dissolved["rime"] // parts


def _winter_brine_example():
    rime = PROPERTIES["rime"]
    cold, warm = winter_brine("cold"), winter_brine("warm")
    ticks = min(rime["max_ticks"], cold * rime["ticks_per_unit"] // 1000)
    text = (f"Three snowballs as they come in one part of water, stirred cold three times, hold {_units(cold / 1000)} Rime a "
            f"part: a draught of **Frostward** for {_seconds(ticks)} s.")
    if warm < _milli(rime["threshold"]):
        text += f" Stirred warm, the same three keep {_units(warm / 1000)}: too weak (it needs {_units(rime['threshold'])})."
    return text


# ------------------------------------------------------------------------------------------------- outcomes and products

def fouled(axis, amount, contaminant):
    """What is left of {amount} milli-units a part of {axis} once {contaminant} milli-units a part foul it: the
    property's fouling, in units, for each unit of contaminant, never more than there is (Java: Property.fouled, taken
    in Outcome.of before the level and duration; Concordance Chunk C, C-02, C-P7)."""
    taken = contaminant * _milli(PROPERTIES[axis].get("fouling", 0)) // 1000
    return max(0, amount - taken)


def judge(key, amount):
    """(status, amplifier, ticks) of a dose holding {amount} milli-units a part of property {key}, or None below its
    threshold (Java: Property.level and ticks)."""
    info = PROPERTIES[key]
    threshold, per_level = _milli(info["threshold"]), _milli(info["per_level"])
    if amount < threshold:
        return None
    level = min(info["max_level"], 1 + (amount - threshold) // per_level)
    return info["status"], level - 1, min(info["max_ticks"], amount * info["ticks_per_unit"] // 1000)


def bottle(operations, drawn=0):
    """The dose (milli-units a part, by axis) and the contaminant a part of the bottle drawn after {drawn} others from the
    mixture {operations} make (Java: Mixture.bottle). Every dose of a batch is the same share, so they stack: what is
    left after a bottle holds exactly that share for each part still in it, and what did not divide evenly is lost."""
    parts, _, dissolved, contaminant = simulate(operations)
    for _ in range(drawn):
        dose = {axis: dissolved[axis] // parts for axis in AXES}
        dissolved = {axis: dose[axis] * (parts - 1) for axis in AXES}
        contaminant = contaminant // parts * (parts - 1)
        parts -= 1
    return {axis: dissolved[axis] // parts for axis in AXES}, contaminant // parts


def outcome(dose, contaminant):
    """What a dose does, as Java's alchemy/Outcome.of works it out: {axis, or "contaminant": (status, amplifier, ticks), or
    None when too weak}, each axis judged after its fouling."""
    out = {axis: judge(axis, fouled(axis, dose[axis], contaminant)) for axis in AXES if dose[axis]}
    if contaminant:
        out["contaminant"] = judge("contaminant", contaminant)
    return out


# Vanilla's Regeneration heals one health every 50 >> amplifier ticks (26.3's
# RegenerationMobEffect.shouldApplyEffectTickThisTick, read from the decompiled sources;
# LairEncounterGameTests.regenerationHealsOnItsVanillaInterval measures it).
REGENERATION_INTERVAL = {0: 50, 1: 25}


def realised(outcomes, axis):
    """What the doses {outcomes} give in all through {axis}: health healed for Regeneration, ticks of effect otherwise."""
    total = 0
    for each in outcomes:
        effect = each.get(axis)
        if effect:
            _, amplifier, ticks = effect
            total += ticks // REGENERATION_INTERVAL[amplifier] if effect[0] == "minecraft:regeneration" else ticks
    return total


def stillroot_poultice(bone_meal=0, water=1):
    """The Stillroot Poultice's process: two Mendvetch as they come in {water} parts of water, {bone_meal} bone meal
    stirred in with them (a dirty batch), then four warm stirs."""
    return ([("water", water)] + [("add", "mendvetch", "raw")] * 2 + [("add", "bone_meal", "raw")] * bone_meal
            + [("stir", "warm")] * 4)


STILLROOT_BONE_MEAL = 3  # the dirtiest batch the audit and ConcordanceAlchemyGameTests work


def poultice_verdance():
    """Verdance a part (milli-units, after fouling) of a poultice with 0 to STILLROOT_BONE_MEAL bone meal:
    ConcordanceAlchemyGameTests.aStillrootPoulticeRepeatsAndADirtyBatchHealsLess's `long[] verdance` (check_alchemy)."""
    out = []
    for bone_meal in range(STILLROOT_BONE_MEAL + 1):
        dose, contaminant = bottle(stillroot_poultice(bone_meal))
        out.append(fouled("verdance", dose["verdance"], contaminant))
    return out


KILN_DRAUGHT = [("water", 1), ("add", "magma_cream", "raw")] + [("stir", "hot")] * 2
KILN_DRAUGHT_SUGAR = [("water", 1)] + [("add", "sugar", "raw")] * 2 + [("stir", "hot")] * 2

# Concordance Chunk C, C-02 (C-D1, C-P2): the workshop's products. A product is a brew the crucible already makes, named
# on the stack (vanilla's item name) when it is of this form, carries the status its property brews and carries nothing
# harmful (Java: alchemy/Product.java, AlchemyCatalog.product, Alchemy.brew); data concordance/product/<key>.json. Never
# an item, an ingredient or a Scale material (check_mod_data.py check_workshop_products). "processes" are the worked
# examples the codex shows and the audits replay, each from one part of water; "jei" the ingredients whose JEI page
# names the product (Winter Brine's are named by RIME_JEI).
PRODUCTS = {
    "stillroot_poultice": {"name": "Stillroot Poultice", "form": "salve", "property": "verdance",
                           "codex": "alembic/stillroot_poultice", "jei": ["mendvetch"],
                           "processes": {"mendvetch": stillroot_poultice()}},
    "kiln_draught": {"name": "Kiln Draught", "form": "draught", "property": "ember", "codex": "alembic/kiln_draught",
                     "jei": ["magma_cream", "sugar"],
                     "processes": {"magma_cream": KILN_DRAUGHT, "sugar": KILN_DRAUGHT_SUGAR}},
    # Chunk B's brew, now named on its bottle too (Chunk B's known defect: it read "Draught").
    "winter_brine": {"name": "Winter Brine", "form": "draught", "property": "rime", "codex": "alembic/winter_brine",
                     "jei": [], "processes": {"snowball": WINTER_BRINE + [("stir", "cold")] * WINTER_BRINE_STIRS}},
}
# Boss drops that reach a product's ingredient by recipes alone (not through the Assayer's Scale, whose exchange is
# priced): declared reward routes, not loops, since no brew makes anything (C-P9; check_workshop_products).
PRODUCT_REWARD_ROUTES = {rid("tyrant_scale"): {"minecraft:magma_cream"},  # two magma cream a scale
                         rid("grave_dust"): {rid("mendvetch")},  # two bone meal a dust, and Mendvetch from bone meal
                         # Since main f904df3fd, by tools only: Tatterlace's silk unspins to string, which makes the
                         # household knife; the Iron Dreadnought's coil gives redstone, whose Exchange fuels make red
                         # matter, whose hoe a recipe names, on to sticks and a bronze knife. A knife cuts a bone into
                         # bone meal (Mendvetch) and a stink bottle into spray and its bottle (glucose syrup, then sugar)
                         # and is kept, but the graph (check_mod_data.py _made_from) counts a recipe's tool as an input.
                         rid("gossamer_silk"): {rid("mendvetch"), "minecraft:sugar"},
                         rid("salvaged_coil"): {rid("mendvetch"), "minecraft:sugar"}}
ROMAN = ["I", "II", "III", "IV", "V"]


def product_name_key(key, info):
    """The translation key a product's stack is named with: item.jugcraft.<form>.<key>."""
    return f"item.{MOD}.{info['form']}.{key}"


def product_json(key, info):
    """concordance/product/<key>.json (Java: rules/RulesParser.product)."""
    return {"schema": 1, "form": info["form"], "status": PROPERTIES[info["property"]]["status"], "harmful": False,
            "name": product_name_key(key, info), "codex": info["codex"]}


def _exact(milli):
    return f"{milli // 1000}.{milli % 1000:03d}"


def _fouling_text():
    fouling = [f"{axis.title()} (each unit of contaminant a part takes {_units(PROPERTIES[axis]['fouling'])} of it)"
               for axis in AXES if PROPERTIES[axis].get("fouling")]
    return " Contaminant also fouls " + ", ".join(fouling) + ", before the dose is judged." if fouling else ""


def _poultice_example():
    dose, contaminant = bottle(stillroot_poultice())
    _, amplifier, ticks = outcome(dose, contaminant)["verdance"]
    return (f"Two Mendvetch as they come in one part of water, stirred warm four times, hold {_exact(dose['verdance'])} "
            f"Verdance a part: in a bowl, a **Stillroot Poultice** of Regeneration {ROMAN[amplifier]} for "
            f"{_seconds(ticks)} s. Use it on a friend, or on yourself while sneaking, and the bowl comes back. Any salve "
            f"that brings Regeneration and nothing harmful is a Stillroot Poultice.")


def _clean_batch_example():
    dose, contaminant = bottle(stillroot_poultice(1))
    left = fouled("verdance", dose["verdance"], contaminant)
    _, amplifier, _ = outcome(dose, contaminant)["verdance"]
    two = outcome(*bottle(stillroot_poultice(2)))
    poison = " Two bring Poison as well." if two.get("hollow") else ""
    searing = SEARING_CONTAMINANT * _milli(PROPERTIES["verdance"]["fouling"]) // 1000
    return (f"Contaminant fouls Verdance: each unit of it a part takes {_units(PROPERTIES['verdance']['fouling'])} units "
            f"of Verdance a part before the dose is judged. One bone meal stirred in with the same Mendvetch brings "
            f"{_exact(contaminant)} contaminant and leaves {_exact(left)} Verdance: only Regeneration "
            f"{ROMAN[amplifier]}.{poison} A searing stir adds {_exact(SEARING_CONTAMINANT)} contaminant to the pot, "
            f"which in one part takes {_exact(searing)}. The Assay Glass shows what the contaminant takes; a master's "
            f"glass shows the weaker outcome and names what fouled it.")


def _kiln_example():
    cream, cream_contaminant = bottle(KILN_DRAUGHT)
    sugar, _ = bottle(KILN_DRAUGHT_SUGAR)
    split = outcome(*bottle([("water", 2)] + KILN_DRAUGHT[1:]))["ember"]
    nausea = _milli(PROPERTIES["contaminant"]["threshold"])
    return (f"One magma cream as it comes in one part of water, stirred hot twice, holds {_exact(cream['ember'])} Ember a "
            f"part (and {_exact(cream_contaminant)} contaminant, under the {_exact(nausea)} that brings nausea): a "
            f"**Kiln Draught** of Fire Resistance for {_seconds(outcome(cream, cream_contaminant)['ember'][2])} s. Two "
            f"sugar stirred the same way hold {_exact(sugar['ember'])}: {_seconds(outcome(sugar, 0)['ember'][2])} s. In "
            f"two parts of water the magma cream makes two draughts of {_seconds(split[2])} s each: more bottles, no more "
            f"time.")


# The statuses the properties brew, by name (the codex's property table and the products' JEI lines).
STATUS_NAMES = {"minecraft:night_vision": "Night Vision", "minecraft:regeneration": "Regeneration",
                "minecraft:fire_resistance": "Fire Resistance", FROSTWARD: "Frostward",
                "minecraft:water_breathing": "Water Breathing", "minecraft:poison": "Poison", "minecraft:nausea": "Nausea"}


def _property_table():
    return "\\\n".join(f"- {axis.title()}: {STATUS_NAMES[info['status']]} from {_units(info['threshold'])} a part, "
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
                 f"{SEARING_KEEP // 10}% of the dissolved Radiance and Verdance and fouls the mixture."
                 f" Rime is cold's own: a stir above the cold band keeps only part of the Rime already dissolved "
                 f"({_rime_keeps()})."),
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
                                                                 "that property; too much contaminant brings nausea."
                 + _fouling_text()),
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
                 "against its threshold, and which ingredients put it there. Carried, it also lets the encyclopedia's "
                 "Inspect page read the crucible you look at, at the same level.", rid("assay_glass")),
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
                 f"It never guesses: a missing ingredient or the wrong heat simply waits. The crucible says what it "
                 f"waits for (use it with an empty hand). If the rules change so that a step can no longer be made, "
                 f"the formula stays set, says which step and why, and carries on when they change back. Only someone "
                 f"who could break the crucible by hand may work it, or set or clear its formula."),
            ],
        },
        # Concordance Chunk B, B-08 (B-P44): Winter Brine needs only the Alembic Arts, not Rimekeeping.
        ("alembic", "winter_brine"): {
            "name": "Winter Brine", "x": 4, "y": 2, "icon": "minecraft:snowball", "condition": (art, "observed"),
            "description": "Rime stirred cold: Frostward",
            "pages": [
                ("text", "Winter Brine",
                 f"Rime brews Frostward, and Rime is cold's own: every stir above the cold band thaws part of the Rime "
                 f"already dissolved ({_rime_keeps()}). So stir it with nothing burning beneath the crucible. "
                 + _winter_brine_example()),
                ("text", "Frostward",
                 f"While Frostward lasts you gather no frost: powder snow, the Frost Horn's chill and the Yeti King's breath "
                 f"and blizzard leave none on you, though his blows still land. It lasts "
                 f"{_seconds(PROPERTIES['rime']['ticks_per_unit'])} s for each unit of Rime a part holds, at most "
                 f"{_seconds(PROPERTIES['rime']['max_ticks'])} s, and milk ends it. The Yeti Mitten from his loot, held in "
                 f"the other hand, keeps you warm for as long as you hold it."),
            ]},
        # Concordance Chunk C, C-02 (C-D1, C-P2): the workshop's products, named on the stack by what they carry.
        ("alembic", "stillroot_poultice"): {
            "name": "Stillroot Poultice", "x": 0, "y": 4, "icon": rid("mendvetch"), "condition": (art, "observed"),
            "description": "Mendvetch stirred clean: a healing salve",
            "pages": [
                ("text", "Stillroot Poultice", _poultice_example()),
                ("text", "A Clean Batch", _clean_batch_example()),
            ]},
        ("alembic", "kiln_draught"): {
            "name": "Kiln Draught", "x": 2, "y": 4, "icon": "minecraft:magma_cream", "condition": (art, "observed"),
            "description": "Ember stirred hot: Fire Resistance",
            "pages": [
                ("text", "Kiln Draught", _kiln_example()),
                ("text", "Against the Kiln",
                 f"Under Fire Resistance the Cinder Kiln's slag does not burn and his flame does not take, though his "
                 f"blows, gobs and cinders still land. It lasts {_seconds(PROPERTIES['ember']['ticks_per_unit'])} s a unit "
                 f"of Ember a part, at most {_seconds(PROPERTIES['ember']['max_ticks'])} s. The Salamander Charm from his "
                 f"loot, held in the other hand, keeps hot ground from burning you for as long as you hold it. A Tyrant "
                 f"Scale makes two magma cream."),
            ]},
        # Concordance Chunk C, C-05: the Crucible Stand, on Root and Rivet's foundation.
        ("alembic", "crucible_stand"): {
            "name": "Crucible Stand", "x": 4, "y": 4, "icon": rid("crucible_stand"), "condition": (art, "understood"),
            "description": "Electric heat for the next stir",
            "pages": [
                ("crafting_recipe", "Crucible Stand",
                 "Set an Alembic Crucible on it and run a cable to a side or underneath. Its base is a Rooted Thallite "
                 "Foundation, from the [Root and Rivet](entry://jugcraft:arcane_concordance/circles/root_and_rivet) rite.",
                 rid("crucible_stand")),
                ("text", "Heat for the Next Stir",
                 f"Following a formula it heats the crucible for the next stir: warm {STAND_TARGETS['warm']}, hot "
                 f"{STAND_TARGETS['hot']}, searing {STAND_TARGETS['searing']} degrees, for {STAND_JE['warm']}, "
                 f"{STAND_JE['hot']} and {STAND_JE['searing']} JE a tick; for a cold stir it lets the crucible cool. It "
                 "draws nothing while the crucible waits for anything but heat, while Preserve holds it, or while it is "
                 "already hot enough. With no formula, use it with an empty hand to set off, warm, hot or searing."),
                ("text", "Water by Pipe",
                 "An Electric Pump on a water source, piped into the crucible, fills its tank for the formula's water "
                 "steps; the tank takes water only and gives none back. A campfire or any other heat beneath a crucible "
                 "works as it always has."),
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
    tags.add("block", "minecraft:mineable/pickaxe", rid("crucible_stand"))


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


def _stand_box(frm, to, texture, emissive=False):
    """One box of the Crucible Stand, every face drawn (closed geometry), its UVs vanilla's from its place."""
    element = {"from": frm, "to": to,
               "faces": {d: {"texture": f"#{texture}"} for d in ("north", "east", "south", "west", "up", "down")}}
    if emissive:
        element["light_emission"] = 15
    return element


def stand_elements(lit):
    """The Crucible Stand (Concordance Chunk C, C-05): Root and Rivet's thallite slab, four treated-iron posts carrying the
    treated-iron deck the crucible stands on, silver-grey cladding round its works, and the owner's copper coil, recessed
    between cladding and deck, drawn full-bright on the ticks it pays (lit; C-P48). The coil's top lies under the deck and
    its bottom on the cladding, so only its sides show: the owner's coil seen from its side on every face."""
    posts = [_stand_box([x, 2, z], [x + 2, 13, z + 2], "frame") for x in (1, 13) for z in (1, 13)]
    return ([_stand_box([0, 0, 0], [16, 2, 16], "foundation")] + posts
            + [_stand_box([3, 2, 3], [13, 10, 13], "plates"),
               _stand_box([4, 10, 4], [12, 13, 12], "coil", emissive=lit),
               _stand_box([1, 13, 1], [15, 16, 15], "frame")])


def stand_model(lit):
    """The stand's block model with its power sockets on the faces that take JE (STAND_PORTS), none on its seat."""
    from machine_connections import with_model_sockets
    textures = {"particle": rid("block/crucible_stand_frame"), "foundation": rid("block/thallite_block"),
                "plates": rid("block/crucible_stand_plates"), "frame": rid("block/crucible_stand_frame"),
                "coil": rid("block/crucible_stand_coil")}
    return with_model_sockets({"parent": "minecraft:block/block", "textures": textures, "elements": stand_elements(lit)},
                              [((0, 0, 0), face, "power") for face in STAND_PORTS])


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
    # Concordance Chunk C, C-05: the Crucible Stand (lit on the ticks it pays); its item shows the block.
    for lit in (False, True):
        write(assets / "models" / "block" / f"crucible_stand{'_lit' if lit else ''}.json", stand_model(lit))
    write(assets / "blockstates" / "crucible_stand.json",
          {"variants": {"lit=false": {"model": rid("block/crucible_stand")},
                        "lit=true": {"model": rid("block/crucible_stand_lit")}}})
    write(assets / "items" / "crucible_stand.json", {"model": {"type": "minecraft:model", "model": rid("block/crucible_stand")}})
    write(data / "loot_table" / "blocks" / "crucible_stand.json", self_drop("crucible_stand"))
    feature = "concordance"
    recipes = {
        "crucible": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["C C", "CKC", "BBB"],
                     "key": {"C": "minecraft:copper_ingot", "K": "minecraft:cauldron", "B": "minecraft:bricks"},
                     "result": {"id": rid("crucible"), "count": 1}},
        "mortar": {"type": "minecraft:crafting_shapeless", "category": "equipment",
                   "ingredients": ["minecraft:bowl", "minecraft:flint", "minecraft:stone"],
                   "result": {"id": rid("mortar"), "count": 1}},
        "stirring_rod": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" C", "SS"],
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
        # Concordance Chunk C, C-05 (C-P47): on Root and Rivet's foundation; with the machines switch too (its JE) and the
        # thallite switch (the foundation's plate needs both, as Root and Rivet's offerings do: C-P41).
        "crucible_stand": {"fabric:load_conditions": condition("concordance") + condition("machines") + condition("thallite"),
                           "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ICI", "ICI", "IFI"],
                           "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot", "F": rid("rooted_thallite_foundation")},
                           "result": {"id": rid("crucible_stand"), "count": 1}},
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
    # Concordance Chunk C, C-02: the workshop's products (Java: rules/RulesParser.product).
    for key, info in PRODUCTS.items():
        write(data / "concordance" / "product" / f"{key}.json", product_json(key, info))


def recipe_ingredients():
    """For JEI (client/compat/JugcraftJeiPlugin "alchemy_ingredients"): what each ingredient carries, public data;
    never a formula or an outcome."""
    return [{"item": info["item"], "key": key} for key, info in INGREDIENTS.items()]
