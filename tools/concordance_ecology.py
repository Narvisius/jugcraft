"""Roadmap step 14: ecological cultivation and living devices (docs/features/arcane-concordance-ecology.md).

The Greenwardens' practice. A garden is Verdant Beds and what grows in them. Each organism has a niche: a range for
each of five habitat factors (moisture, light, nutrients, diversity and magical disturbance), and grows on random ticks
(vanilla's, or a sprinkler's) at full pace while every factor is ideal, at half pace while some are only tolerable,
and not at all while any is outside its range. Every step costs the bed nutrients and dries it, and only declared
sources put nutrients back: a nitrogen fixer (Mendvetch), the Mulch Maw (waste, at a loss), bone meal and fertilizer.

Four living devices work the garden: the Verdant Heart (producer: nutrients into Verdance), the Mulch Maw (consumer:
waste into nutrients for the poorest bed), the Habitat Gauge (sensor: the habitat as a comparator signal) and the
Gleaner (collector: harvests mature crops for a Verdance each). The rules are data (data/jugcraft/concordance/organism
and disturbance, read by Java's concordance/ecology); the area round a plant is sampled within fixed bounds, at most
SAMPLES_PER_TICK a level a tick, and a sample is reused for FRESH_TICKS.

tools/concordance.py merges these tables into its own. Numbers the Java repeats are checked by tools/check_mod_data.py.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Habitats (Java: ecology/Habitat.java, Sampler.java, SampleBudget.java, Mulch.java, Organism.java).
STAGES = 3  # a crop's steps from planted (age 0) to mature
MAX_NUTRIENTS = 32  # what a bed holds
SAMPLE_RADIUS = 2  # the 5x5 round a plant
SAMPLE_HEIGHT = 1  # and one block above and below it, for disturbance
MAX_DIVERSITY = 8
MAX_DISTURBANCE = 15
READS = (2 * SAMPLE_RADIUS + 1) ** 2 * (2 * SAMPLE_HEIGHT + 2)  # positions one sample reads
FRESH_TICKS = 200  # a bed keeps its sample this long
SAMPLES_PER_TICK = 16  # the most samples one level takes in one tick
MULCH_QUARTERS = 4  # quarters of a nutrient that make one
# Factor limits (Java: ecology/Factor.java).
FACTORS = {"moisture": 7, "light": 15, "nutrients": MAX_NUTRIENTS, "diversity": MAX_DIVERSITY,
           "disturbance": MAX_DISTURBANCE}
# The Verdant Bed (Java: VerdantBedBlockEntity.java, VerdantBedBlock.java).
BONE_MEAL_NUTRIENTS = 2  # one bone meal (or one fertilizer dose) on a bed or a crop in it
AWAKEN_LIMIT = 64  # beds one touch of a Greenwarden wakes (the dormant bed and those joined to it)
WATER_REACH = 4  # water this far round a bed (and one above) keeps it wet, as farmland's
# The Verdant Heart (Java: VerdantHeartBlockEntity.java).
BEAT_TICKS = 200
HEART_CAPACITY = 64
# The Mulch Maw (Java: MulchMawBlockEntity.java).
DIGEST_TICKS = 40
MAW_REACH = 3  # beds this far round it, and one above or below
# The Habitat Gauge (Java: HabitatGaugeBlockEntity.java).
GAUGE_TICKS = 40
# The Gleaner (Java: GleanerBlockEntity.java).
GLEAN_TICKS = 40
GLEAN_REACH = 3
GLEANER_SLOTS = 9
GLEANER_CAPACITY = 16  # Verdance it holds
GLEANER_DRAW = 8  # the most it draws from a Heart at once
HEART_REACH = 4  # Hearts this far away feed it
GLEAN_COST = 1  # Verdance a harvest costs
# What the Mulch Maw eats, in quarters of a nutrient (item tags #jugcraft:mulch/<name>, so data packs can add more).
# Roughly vanilla's composting chances times four, rounded down. Nothing composted gives back what it cost to grow.
MULCH = {
    "quarter": (1, ["minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
                    "minecraft:pumpkin_seeds", "#minecraft:leaves", "#minecraft:saplings", "minecraft:short_grass",
                    "minecraft:fern", "minecraft:kelp", "minecraft:sweet_berries", rid("sunpetal"), rid("dewmoss"),
                    rid("gloamcap"), rid("mendvetch")]),
    "half": (2, [rid("verdant_chaff"), "minecraft:vine", "minecraft:sugar_cane", "minecraft:melon_slice",
                 "minecraft:cactus", "minecraft:moss_carpet", "minecraft:tall_grass"]),
    "three_quarters": (3, ["minecraft:wheat", "minecraft:carrot", "minecraft:potato", "minecraft:beetroot",
                           "minecraft:apple", "minecraft:moss_block", "minecraft:brown_mushroom",
                           "minecraft:red_mushroom", "minecraft:pumpkin", "minecraft:melon", "#minecraft:small_flowers"]),
    "whole": (4, ["minecraft:bread", "minecraft:baked_potato", "minecraft:hay_block", "minecraft:pumpkin_pie",
                  "minecraft:cake"]),
}

GARDEN_SPECIMEN_TAG = f"{MOD}:verdant_specimens"
GARDEN_SPECIMENS = {
    "minecraft:moss_block": "Moss Block", "minecraft:sunflower": "Sunflower", "minecraft:brown_mushroom": "Brown Mushroom",
    "minecraft:lily_pad": "Lily Pad", "minecraft:fern": "Fern",
}
CULTIVATION = rid("cultivation")  # the practice a crop harvested by hand records (Java: Garden.ACTIVITY)

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "verdant_husbandry": {
        "name": "Verdant Husbandry",
        "principle": "verdance",
        "tradition": "greenwardens",
        "stage": "practitioner",
        "icon": rid("sunpetal"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{GARDEN_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{GARDEN_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{GARDEN_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: every crop grown and harvested by your own hand.
            "mastered": [{"type": "practice", "activity": CULTIVATION, "distinct": 4}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Green Fingers", "Examine a living specimen", "task", "minecraft:moss_block"),
            "observed": ("Field Notes", "Examine three different living specimens", "task", "minecraft:fern"),
            "understood": ("Verdant Husbandry", "Understand Verdant Husbandry: study a living specimen at a "
                           "Lampwright's Bench", "goal", rid("verdant_bed")),
            "mastered": ("Greenwarden", "Grow and harvest all four of the Greenwardens' crops by hand", "challenge",
                         rid("verdant_heart")),
        },
        "locked": {"encountered": "Examine a living specimen first",
                   "observed": "Examine three different living specimens first",
                   "understood": "Understand Verdant Husbandry first", "mastered": "Master Verdant Husbandry first"},
    },
}

# ------------------------------------------------------------------------------------------------- organisms

# A niche range is [least, ideal from, ideal to, most]; a factor left out takes any value. "growth" scales a crop's
# chance per random tick as vanilla's growth time does (1.0 is one wheat step's pace).
ORGANISMS = {
    # Sun on its face and company round it: bees come to a varied garden.
    "sunpetal": {"role": "crop", "name": "Sunpetal", "growth": 2.0, "cost": 1, "replant": 1, "produce": 2, "chaff": 1,
                 "niche": {"moisture": [2, 4, 7, 7], "light": [11, 13, 15, 15], "nutrients": [1, 3, 32, 32],
                           "diversity": [1, 3, 8, 8], "disturbance": [0, 0, 2, 5]},
                 "summary": "Wants bright sun (13 or more), a moist bed and different plants round it."},
    # Shade and wet: it drinks what the bed holds.
    "dewmoss": {"role": "crop", "name": "Dewmoss", "growth": 1.5, "cost": 1, "replant": 1, "produce": 2, "chaff": 1,
                "niche": {"moisture": [5, 7, 7, 7], "light": [0, 0, 9, 12], "nutrients": [1, 2, 32, 32]},
                "summary": "Wants shade (9 or less) and a soaking bed."},
    # Dark, and fed by working magic nearby.
    "gloamcap": {"role": "crop", "name": "Gloamcap", "growth": 2.5, "cost": 2, "replant": 1, "produce": 1, "chaff": 1,
                 "niche": {"moisture": [2, 3, 7, 7], "light": [0, 0, 6, 9], "nutrients": [2, 4, 32, 32],
                           "disturbance": [1, 3, 10, 15]},
                 "summary": "Wants the dark (6 or less), a rich bed and working magic close by."},
    # A vetch: it costs nothing and puts nutrients back, but quiet magic only.
    "mendvetch": {"role": "crop", "name": "Mendvetch", "growth": 1.5, "cost": 0, "fix": 1, "replant": 1, "produce": 1,
                  "chaff": 1,
                  "niche": {"moisture": [1, 3, 7, 7], "light": [8, 11, 15, 15], "disturbance": [0, 0, 3, 6]},
                  "summary": "A nitrogen fixer: each step gives a nutrient to the poorest bed round it. Wants light "
                             "and quiet."},
    # The producer: a diverse, fed and lit garden beats Verdance out of its bed.
    "verdant_heart": {"role": "producer", "name": "Verdant Heart", "cost": 1, "thriving": 2, "tolerating": 1,
                      "niche": {"moisture": [2, 4, 7, 7], "light": [8, 12, 15, 15], "nutrients": [1, 4, 32, 32],
                                "diversity": [2, 3, 8, 8], "disturbance": [0, 0, 6, 10]},
                      "summary": "Wants light, a fed bed and at least two other kinds of plant round it."},
}
CROPS = [key for key, info in ORGANISMS.items() if info["role"] == "crop"]

# What working magic gives off into the habitats round it (a sample sums the blocks round a plant, capped at 15).
DISTURBANCE = {
    "circle_anchor": {"blocks": [rid("circle_anchor")], "value": 3},
    "ley_pylon": {"blocks": [rid("ley_pylon")], "value": 2},
    "crucible": {"blocks": [rid("crucible")], "value": 2},
    "verdant_heart": {"blocks": [rid("verdant_heart")], "value": 2},
    "lumen": {"blocks": [rid("lumen_sconce"), rid("lumen_mote")], "value": 1},
    "warding_stone": {"blocks": [rid("warding_stone")], "value": 1},
    "amethyst": {"blocks": ["minecraft:amethyst_cluster", "minecraft:budding_amethyst"], "value": 1},
}

# A garden's Verdance can power a circle: a Heart beside a Ley Pylon pours into it, losing a third.
CONVERSIONS = {
    "verdance_to_ley": {"from": {"resource": "essence/verdance", "amount": 3},
                        "to": {"resource": "ley_charge", "amount": 2}},
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "sunpetal": {"name": "Sunpetal",
                 "tooltip": "Plant it in a Verdant Bed; harvested, an alchemical ingredient bright with Radiance."},
    "dewmoss": {"name": "Dewmoss", "tooltip": "Plant it in a Verdant Bed in the shade; harvested, full of Tide."},
    "gloamcap": {"name": "Gloamcap", "tooltip": "Plant it in a Verdant Bed in the dark near working magic."},
    "mendvetch": {"name": "Mendvetch", "tooltip": "Plant it in a Verdant Bed: it gives nutrients back to the beds round it."},
    "verdant_chaff": {"name": "Verdant Chaff", "tooltip": "What a harvest leaves: feed it to a Mulch Maw."},
}
DEVICES = {
    "verdant_bed": {"name": "Verdant Bed"},
    "verdant_heart": {"name": "Verdant Heart"},
    "mulch_maw": {"name": "Mulch Maw"},
    "habitat_gauge": {"name": "Habitat Gauge"},
    "gleaner": {"name": "Gleaner"},
}
# Device items: their tooltips (the block items are listed in ITEMS by concordance.py through DEVICE_ITEMS).
DEVICE_TOOLTIPS = {
    "verdant_bed": "Soil for the Greenwardens' crops: it holds water like farmland and up to %s nutrients. A "
                   "Greenwarden's touch wakes it." % MAX_NUTRIENTS,
    "verdant_heart": "Set on a Verdant Bed: in a lit, fed and varied garden it beats nutrients into Verdance.",
    "mulch_maw": "Feed it plant matter: it gives the nutrients to the poorest Verdant Bed round it.",
    "habitat_gauge": "Set on a Verdant Bed: it reads the habitat there as a comparator signal. Use a crop on it to "
                     "ask about that crop.",
    "gleaner": "Harvests ripe crops round it for a Verdance each, drawn from a Verdant Heart nearby.",
}
# A crop's block is "<crop>_crop" (as Jugcraft's other crops); its item, which plants it, is "<crop>".
CROP_BLOCKS = {f"{key}_crop": {"name": ORGANISMS[key]["name"]} for key in CROPS}
BLOCKS = dict(DEVICES)
# Blocks with no item of their own: a crop is planted by its item.
ITEMLESS_BLOCKS = dict(CROP_BLOCKS)

# ------------------------------------------------------------------------------------------------- text

MESSAGES = {
    "garden.unknown": "You have not learned Verdant Husbandry (the codex says how)",
    "garden.dormant": "Dormant: a Greenwarden must wake it (use it with an empty hand)",
    "garden.awakened": "Awake: %s",
    "garden.already_awake": "It is already awake",
    "garden.disabled": "The Concordance is switched off on this server",
    "garden.bed": "Verdant Bed: moisture %s, nutrients %s / %s",
    "garden.fed": "Fed the bed: nutrients %s / %s",
    "garden.bed_full": "The bed holds all the nutrients it can",
    "garden.crop": "%s: step %s of %s, %s",
    "garden.no_bed": "It grows only in a Verdant Bed",
    "garden.harvested": "Harvested %s",
    "garden.heart": "Verdant Heart: %s / %s Verdance, %s",
    "garden.maw": "Mulch Maw: %s waiting, %s quarters digested, %s",
    "garden.gauge": "Habitat Gauge: %s, signal %s",
    "garden.gauge_mode": "Now reading: %s",
    "garden.gauge_attuned": "Now judging the habitat for %s",
    "garden.gleaner": "Gleaner: %s / %s Verdance, %s",
    "garden.not_owner": "Only its keeper (or their party) may change it",
}

# The simulation's own words (compose.jugcraft.*, shown through ComposeText like the compiler's).
TEXT = {
    "ecology.short": "%s %s: too little (needs at least %s)",
    "ecology.excess": "%s %s: too much (at most %s)",
    "ecology.tolerable": "%s %s: tolerable (best from %s to %s)",
    "ecology.habitat": "Moisture %s, light %s, nutrients %s, diversity %s, disturbance %s",
    "ecology.growth.thriving": "thriving",
    "ecology.growth.tolerating": "growing slowly",
    "ecology.growth.stalled": "not growing",
    "ecology.mature": "ripe",
    "ecology.status.working": "working",
    "ecology.status.dormant": "dormant",
    "ecology.status.disabled": "switched off",
    "ecology.status.stalled": "the habitat holds it back",
    "ecology.status.starved": "its bed is too poor",
    "ecology.status.full": "full",
    "ecology.status.no_bed": "not on a Verdant Bed",
    "ecology.status.idle": "nothing to do",
    "ecology.status.no_verdance": "no Verdance (a Verdant Heart within %s blocks feeds it)" % HEART_REACH,
    "ecology.status.beds_full": "every bed round it is full",
    "ecology.status.hungry": "nothing to eat",
    "ecology.status.waiting": "waiting for a reading",
    "ecology.status.keeper_away": "waiting for its keeper (it harvests only while they are here)",
    "ecology.gauge.suitability": "suitability",
}
for _factor in FACTORS:
    TEXT[f"factor.{_factor}"] = _factor.title()

TOOLTIPS = {
    "garden_specimen": "Living specimen: sneak and use to examine it",
    "jade.habitat": "Moisture %s, light %s, nutrients %s",
    "jade.area": "Diversity %s, disturbance %s",
    "jade.nutrients": "Nutrients: %s / %s",
    "jade.verdance": "Verdance: %s / %s",
    "jade.growth": "%s",
    "jade.reason": "%s",
    "jade.gauge": "Reading %s: signal %s",
    "jade.maw": "%s quarters digested",
    "jade.dormant": "Dormant: a Greenwarden must wake it",
}


def lang_entries(lang):
    lang[f"tag.item.{GARDEN_SPECIMEN_TAG.replace(':', '.')}"] = "Living Specimens"
    for key, text in TEXT.items():
        lang[f"compose.{MOD}.{key}"] = text
    for key in ("verdant_bed", "verdant_heart", "mulch_maw", "habitat_gauge", "gleaner"):
        lang[f"config.jade.plugin_{MOD}.{key}"] = DEVICES[key]["name"]
    lang[f"config.jade.plugin_{MOD}.organism"] = "Greenwarden crops"
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"jei.{MOD}.garden.compost"] = "Mulch Maw: %s quarters of a nutrient"
    for name in MULCH:
        lang[f"tag.item.{MOD}.mulch.{name}"] = "Mulch: " + name.replace("_", " ")


# ------------------------------------------------------------------------------------------------- codex

def _range(factor, values):
    least, low, high, most = values
    top = FACTORS[factor]
    if (least, low, high, most) == (0, 0, top, top):
        return None
    if low == least and high == most:
        return f"{factor} {least} to {most}"
    parts = []
    if low > 0 or least > 0:
        parts.append(f"at least {least}" + (f" (best {low}+)" if low > least else ""))
    if most < top or high < top:
        parts.append(f"at most {most}" + (f" (best up to {high})" if high < most else ""))
    return f"{factor} " + ", ".join(parts)


def niche_text(key):
    info = ORGANISMS[key]
    return "; ".join(text for factor in FACTORS if factor in info["niche"]
                     for text in [_range(factor, info["niche"][factor])] if text)


def _crop_table():
    rows = []
    for key in CROPS:
        info = ORGANISMS[key]
        cost = f"costs {info['cost']} nutrient{'s' if info['cost'] != 1 else ''} a step" if info["cost"] \
            else f"gives {info['fix']} a step"
        rows.append(f"- **{info['name']}**: {niche_text(key)}. It {cost}; harvested, {info['produce']} and "
                    f"{info['chaff']} chaff.")
    return "\\\n".join(rows)


def codex():
    art = "verdant_husbandry"
    specimens = "\\\n".join(f"- {name}" for name in GARDEN_SPECIMENS.values())
    disturbance = ", ".join(f"{', '.join(b.split(':')[1].replace('_', ' ') for b in info['blocks'])} {info['value']}"
                            for info in DISTURBANCE.values())
    heart = ORGANISMS["verdant_heart"]
    return {
        ("garden", "verdant_husbandry"): {
            "name": "Verdant Husbandry", "x": 0, "y": 0, "icon": rid("sunpetal"), "condition": None,
            "description": "Gardens: research",
            "pages": [
                ("text", "Verdant Husbandry",
                 "Once you understand First Light, look closely at living things. **Sneak and use** one of these to "
                 "examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 "Your touch now wakes Verdant Beds and living devices. To **master** Verdant Husbandry, harvest each "
                 "of the four crops by hand.", (art, "understood")),
                ("text", "Mastered", "You know a garden as well as it knows itself.", (art, "mastered")),
            ],
        },
        ("garden", "habitats"): {
            "name": "Habitats", "x": 2, "y": 0, "icon": rid("verdant_bed"), "condition": (art, "encountered"),
            "description": "Five factors, and where each comes from",
            "pages": [
                ("crafting_recipe", "Verdant Bed",
                 f"The Greenwardens' crops grow only in a Verdant Bed, and only once a Greenwarden has woken it "
                 f"(an empty hand wakes it and up to {AWAKEN_LIMIT} beds joined to it). It holds up to "
                 f"{MAX_NUTRIENTS} nutrients.", rid("verdant_bed")),
                ("text", "Five Factors",
                 "**Moisture** (0 to 7): water within four blocks, rain, a wet sprinkler or a canteen keep a bed wet; "
                 "every step of growth dries it by one.\\\n**Light** (0 to 15): sun or lamps at the plant, as the "
                 "server's light has it; never the time of day.\\\n**Nutrients**: what the bed holds. Growth spends "
                 "them.\\\n**Diversity**: the different plants in the 5x5 round it, itself included.\\\n"
                 "**Disturbance**: working magic round it."),
                ("text", "Where They Come From",
                 f"Nutrients come only from declared sources: **Mendvetch** (fixed from the air), the **Mulch Maw** "
                 f"(waste, at a loss), **bone meal** ({BONE_MEAL_NUTRIENTS} a dose, on a bed or a crop in it) and "
                 f"**fertilizer**. A broken bed loses what it held.\\\n\\\nDisturbance: {disturbance}."),
                ("text", "How It Grows",
                 "A crop grows on random ticks (and a sprinkler's) at full pace while every factor is ideal, at half "
                 "pace while some are only tolerable, and not at all while one is outside its range. Look at it "
                 "(or use it with an empty hand) to see which."),
            ],
        },
        ("garden", "organisms"): {
            "name": "Organisms", "x": 4, "y": 0, "icon": rid("gloamcap"), "condition": (art, "observed"),
            "description": "Four crops and their niches",
            "pages": [
                ("text", "Four Crops", _crop_table()),
                ("text", "Harvest",
                 f"A ripe crop (step {STAGES}) is harvested with an empty hand, or by a Gleaner: its produce and "
                 f"Verdant Chaff, and it falls back to step 1 to grow again. A Mendvetch next to other crops also "
                 f"speeds them, as legumes do."),
            ] + [("crafting_recipe", ORGANISMS[key]["name"],
                  f"Your first {ORGANISMS[key]['name']} comes from the crafting grid; after that, its harvests give "
                  f"more.", rid(key)) for key in CROPS],
        },
        ("garden", "devices"): {
            "name": "Living Devices", "x": 2, "y": 2, "icon": rid("verdant_heart"), "condition": (art, "understood"),
            "description": "A producer, a consumer, a sensor and a collector",
            "pages": [
                ("crafting_recipe", "Verdant Heart",
                 f"Set on a bed, it beats every {BEAT_TICKS // 20} seconds: in its niche it spends {heart['cost']} "
                 f"nutrient and makes {heart['thriving']} Verdance ({heart['tolerating']} while a factor is merely "
                 f"tolerable), up to {HEART_CAPACITY}. Beside a Ley Pylon it pours in, 3 Verdance for 2 Ley Charge. "
                 f"Its niche: {niche_text('verdant_heart')}.", rid("verdant_heart")),
                ("crafting_recipe", "Mulch Maw",
                 f"Feed it plant matter (by hand or hopper): seeds, leaves and the crops are a quarter of a nutrient, "
                 f"chaff, vines and cane a half, fruit and roots three quarters, bread and hay a whole one. Every "
                 f"{DIGEST_TICKS // 20} seconds it eats one and, for every {MULCH_QUARTERS} quarters, gives a nutrient "
                 f"to the poorest bed within {MAW_REACH} blocks. Nothing composted gives back what it cost to grow.",
                 rid("mulch_maw")),
                ("crafting_recipe", "Habitat Gauge",
                 "Set on a bed, it reads the habitat there as a comparator signal: use it with an empty hand to "
                 "choose a factor, or use a crop on it to read how well that crop would grow (15 thriving, 8 slowly, "
                 "0 not at all).", rid("habitat_gauge")),
                ("crafting_recipe", "Gleaner",
                 f"Harvests ripe crops within {GLEAN_REACH} blocks into its {GLEANER_SLOTS} slots (hoppers take "
                 f"them out), {GLEAN_COST} Verdance a harvest, drawn from a Verdant Heart within {HEART_REACH} "
                 f"blocks.", rid("gleaner")),
            ],
        },
    }


CATEGORY = {"garden": {"name": "Garden", "icon": rid("sunpetal"), "sort": 6,
                       "description": "The Greenwardens' practice: habitats, cultivation and living devices"}}


# ------------------------------------------------------------------------------------------------- data and assets

def organism_json(key):
    info = ORGANISMS[key]
    out = {"schema": 1, "role": info["role"], "block": rid(f"{key}_crop" if info["role"] == "crop" else key)}
    if info["role"] == "crop":
        out.update({"item": rid(key), "growth": info["growth"], "cost": info["cost"]})
        if info.get("fix"):
            out["fix"] = info["fix"]
        out.update({"replant": info["replant"], "produce": info["produce"], "chaff": info["chaff"]})
    else:
        out.update({"cost": info["cost"], "thriving": info["thriving"], "tolerating": info["tolerating"]})
    out["niche"] = info["niche"]
    return out


def tags(tags):
    for ref in GARDEN_SPECIMENS:
        tags.add("item", GARDEN_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/shovel", rid("verdant_bed"))
    for key in ("verdant_heart", "mulch_maw", "habitat_gauge", "gleaner"):
        tags.add("block", "minecraft:mineable/axe", rid(key))
    # A Verdant Bed is good soil for any crop, and is watered like farmland.
    tags.add("block", "minecraft:supports_crops", rid("verdant_bed"))
    tags.add("block", "minecraft:grows_crops", rid("verdant_bed"))
    for key in CROPS:
        # Crops: fertilizer, the crop harvester and the crows treat them as crops.
        tags.add("block", "minecraft:crops", rid(f"{key}_crop"))
    tags.add("block", f"{MOD}:nitrogen_fixing_crops", rid("mendvetch_crop"))
    for name, (_quarters, values) in MULCH.items():
        for value in values:
            tags.add("item", f"{MOD}:mulch/{name}", value)


def crop_models(write, assets):
    for key in CROPS:
        for age in range(STAGES + 1):
            write(assets / "models" / "block" / f"{key}_stage{age}.json",
                  {"parent": "minecraft:block/crop", "textures": {"crop": rid(f"block/{key}_stage{age}")}})
        write(assets / "blockstates" / f"{key}_crop.json",
              {"variants": {f"age={age}": {"model": rid(f"block/{key}_stage{age}")} for age in range(STAGES + 1)}})


def crop_loot(key):
    """Unripe: the crop back. Ripe: that, its produce and its chaff."""
    info = ORGANISMS[key]
    ripe = {"type": "minecraft:match_block", "blocks": rid(f"{key}_crop"), "state": {"age": str(STAGES)}}
    return {"type": "minecraft:block", "modifier": {"type": "minecraft:explosion_decay"}, "pools": [
        {"entries": [{"type": "minecraft:item", "name": rid(key)}], "rolls": 1},
        {"entries": [{"type": "minecraft:item", "name": rid(key),
                      "modifier": [{"type": "minecraft:set_count", "count": info["produce"]}]}], "rolls": 1, "condition": ripe},
        {"entries": [{"type": "minecraft:item", "name": rid("verdant_chaff"),
                      "modifier": [{"type": "minecraft:set_count", "count": info["chaff"]}]}], "rolls": 1, "condition": ripe},
    ], "random_sequence": rid(f"blocks/{key}_crop")}


def fusion_bed_model(name):
    """The Verdant Bed in the Fusion built-in pack: its top's wooden rim opens towards neighbouring beds, so a row of
    beds reads as one long bed."""
    return {"loader": "fusion:model", "type": "connecting", "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": rid(f"block/{name}_top_connected"), "side": rid(f"block/{name}_side"),
                         "bottom": "minecraft:block/dirt"},
            "connections": {"type": "is_same_block"}}


def write_all(write, assets, data, lang, condition, self_drop, packs):
    lang_entries(lang)
    # The bed: a soil cube, dry or wet; with Fusion, rows of beds join.
    for wet in (False, True):
        name = "verdant_bed_wet" if wet else "verdant_bed"
        write(assets / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": rid(f"block/{name}_top"), "side": rid(f"block/{name}_side"), "bottom": "minecraft:block/dirt"}})
    write(assets / "blockstates" / "verdant_bed.json", {"variants": {
        f"moisture={m}": {"model": rid("block/verdant_bed_wet" if m == 7 else "block/verdant_bed")} for m in range(8)}})
    write(assets / "items" / "verdant_bed.json", {"model": {"type": "minecraft:model", "model": rid("block/verdant_bed")}})
    write(data / "loot_table" / "blocks" / "verdant_bed.json", self_drop("verdant_bed"))
    import concordance_ritual_art
    for name in ("verdant_bed", "verdant_bed_wet"):
        write(assets / "textures" / "block" / f"{name}_top_connected.png.mcmeta", concordance_ritual_art.FUSION_METADATA)
        write(packs / "fusion_textures" / "assets" / MOD / "models" / "block" / f"{name}.json", fusion_bed_model(name))
    # Crops: vanilla crop models, one per step; the item plants it.
    crop_models(write, assets)
    for key in CROPS:
        write(data / "loot_table" / "blocks" / f"{key}_crop.json", crop_loot(key))
    for item in CROPS + ["verdant_chaff", "verdant_heart", "mulch_maw", "habitat_gauge", "gleaner"]:
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    # The living devices GeckoLib draws (their block models are only the breaking particles).
    for key, particle in (("verdant_heart", "block/verdant_heart"), ("mulch_maw", "block/mulch_maw"), ("gleaner", "block/gleaner")):
        write(assets / "geckolib" / "models" / "block" / f"{key}.geo.json", GEO[key]())
        write(assets / "geckolib" / "animations" / "block" / f"{key}.animation.json", ANIMATIONS[key]())
        write(assets / "models" / "block" / f"{key}.json", {"textures": {"particle": rid(particle)}})
        write(assets / "blockstates" / f"{key}.json", {"variants": {"": {"model": rid(f"block/{key}")}}})
        write(data / "loot_table" / "blocks" / f"{key}.json", self_drop(key))
    # The gauge: a stalk and a bulb that opens while it gives a signal.
    for open_ in (False, True):
        name = "habitat_gauge_open" if open_ else "habitat_gauge"
        write(assets / "models" / "block" / f"{name}.json", {"parent": "minecraft:block/block", "textures": {
            "particle": rid("block/habitat_gauge_stalk"), "stalk": rid("block/habitat_gauge_stalk"),
            "bulb": rid("block/habitat_gauge_bulb_open" if open_ else "block/habitat_gauge_bulb")},
            "elements": gauge_elements(open_)})
    write(assets / "blockstates" / "habitat_gauge.json", {"variants": {
        "open=false": {"model": rid("block/habitat_gauge")}, "open=true": {"model": rid("block/habitat_gauge_open")}}})
    write(data / "loot_table" / "blocks" / "habitat_gauge.json", self_drop("habitat_gauge"))
    feature = "concordance"
    recipes = {
        "verdant_bed": {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["MBM", "DDD"],
                        "key": {"M": "minecraft:moss_block", "B": "minecraft:bone_meal", "D": "minecraft:dirt"},
                        "result": {"id": rid("verdant_bed"), "count": 3}},
        "sunpetal": {"type": "minecraft:crafting_shapeless", "category": "misc",
                     "ingredients": ["minecraft:sunflower", "minecraft:amethyst_shard", "minecraft:bone_meal"],
                     "result": {"id": rid("sunpetal"), "count": 2}},
        "dewmoss": {"type": "minecraft:crafting_shapeless", "category": "misc",
                    "ingredients": ["minecraft:moss_block", "minecraft:kelp", "minecraft:amethyst_shard"],
                    "result": {"id": rid("dewmoss"), "count": 2}},
        "gloamcap": {"type": "minecraft:crafting_shapeless", "category": "misc",
                     "ingredients": ["minecraft:brown_mushroom", "minecraft:glow_lichen", "minecraft:amethyst_shard"],
                     "result": {"id": rid("gloamcap"), "count": 2}},
        "mendvetch": {"type": "minecraft:crafting_shapeless", "category": "misc",
                      "ingredients": ["minecraft:wheat_seeds", "minecraft:bone_meal", "minecraft:amethyst_shard"],
                      "result": {"id": rid("mendvetch"), "count": 2}},
        "verdant_heart": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SVS", "AGA", " B "],
                          "key": {"S": rid("sunpetal"), "V": rid("mendvetch"), "A": "minecraft:amethyst_shard",
                                  "G": "minecraft:glow_berries", "B": rid("verdant_bed")},
                          "result": {"id": rid("verdant_heart"), "count": 1}},
        "mulch_maw": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["D D", "DCD", " B "],
                      "key": {"D": rid("dewmoss"), "C": "minecraft:composter", "B": rid("verdant_bed")},
                      "result": {"id": rid("mulch_maw"), "count": 1}},
        "habitat_gauge": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": [" G ", " C ", " R "],
                          "key": {"G": rid("gloamcap"), "C": "minecraft:comparator", "R": "minecraft:copper_ingot"},
                          "result": {"id": rid("habitat_gauge"), "count": 1}},
        "gleaner": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" V ", "VHV", " P "],
                    "key": {"V": "minecraft:vine", "H": "minecraft:hopper", "P": "minecraft:flower_pot"},
                    "result": {"id": rid("gleaner"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key in ORGANISMS:
        write(data / "concordance" / "organism" / f"{key}.json", organism_json(key))
    for key, info in DISTURBANCE.items():
        write(data / "concordance" / "disturbance" / f"{key}.json", dict({"schema": 1}, **info))


def compost_view():
    """For JEI (client/compat/JugcraftJeiPlugin "garden"): the organisms' items and chaff, and what the Maw makes of
    them. Public numbers; never a habitat."""
    return [{"tag": f"{MOD}:mulch/{name}", "quarters": quarters} for name, (quarters, _values) in MULCH.items()]


# ------------------------------------------------------------------------------------------------- the gauge's model

def gauge_elements(open_):
    """A copper-ringed stalk with a bulb on top; open, the bulb's petals part to show its glowing heart."""
    def box(f, t, tex, emissive=False):
        element = {"from": f, "to": t, "faces": {d: {"texture": f"#{tex}"} for d in
                                                ("north", "east", "south", "west", "up", "down")}}
        if emissive:
            element["light_emission"] = 10
        return element
    elements = [box([6, 0, 6], [10, 1, 10], "stalk"), box([7, 1, 7], [9, 9, 9], "stalk")]
    if open_:
        elements += [box([5, 9, 5], [11, 12, 11], "bulb", emissive=True),
                     box([4, 11, 7], [5, 14, 9], "bulb"), box([11, 11, 7], [12, 14, 9], "bulb"),
                     box([7, 11, 4], [9, 14, 5], "bulb"), box([7, 11, 11], [9, 14, 12], "bulb")]
    else:
        elements += [box([5.5, 9, 5.5], [10.5, 14, 10.5], "bulb"), box([7, 14, 7], [9, 15, 9], "bulb")]
    return elements


# ------------------------------------------------------------------------------------------------- GeckoLib bodies

def _cube(uv, name, origin, size):
    u, v = uv[name]
    return {"origin": origin, "size": size, "uv": [u, v]}


def _geo(identifier, bones, height=1.5):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{identifier}", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": height, "visible_bounds_offset": [0, 0.75, 0]},
        "bones": bones}]}


# Box-UV origins in each device's 64x64 sheet; a cube of size (w, h, d) uses (2(w + d)) x (d + h) from there
# (tools/concordance_ecology_art.py paints the same regions).
HEART_UV = {"root": (0, 0), "stem": (0, 9), "leaf": (16, 9), "heart": (0, 20), "crown": (40, 0)}
HEART_SIZES = {"root": (8, 1, 8), "stem": (2, 6, 2), "leaf": (6, 1, 3), "heart": (8, 7, 8), "crown": (4, 2, 4)}
MAW_UV = {"pot": (0, 0), "jaw": (0, 24), "tooth": (48, 0), "tongue": (40, 24)}
MAW_SIZES = {"pot": (12, 6, 12), "jaw": (12, 5, 6), "tooth": (1, 2, 1), "tongue": (4, 1, 6)}
GLEANER_UV = {"pot": (0, 0), "soil": (0, 14), "stem": (40, 0), "arm": (0, 24), "hand": (24, 24)}
GLEANER_SIZES = {"pot": (8, 6, 8), "soil": (6, 1, 6), "stem": (2, 6, 2), "arm": (2, 2, 9), "hand": (4, 3, 3)}


def heart_geo():
    """The Verdant Heart: a rooted stem holding a leafy, veined heart that the beat animation swells."""
    uv = HEART_UV
    return _geo("verdant_heart", [
        {"name": "root", "pivot": [0, 0, 0], "cubes": [_cube(uv, "root", [-4, 0, -4], [8, 1, 8])]},
        {"name": "stem", "parent": "root", "pivot": [0, 1, 0], "cubes": [_cube(uv, "stem", [-1, 1, -1], [2, 6, 2])]},
        {"name": "leaves", "parent": "stem", "pivot": [0, 4, 0], "cubes": [
            _cube(uv, "leaf", [1, 4, -1.5], [6, 1, 3]), _cube(uv, "leaf", [-7, 4, -1.5], [6, 1, 3])]},
        {"name": "heart", "parent": "stem", "pivot": [0, 10, 0], "cubes": [_cube(uv, "heart", [-4, 7, -4], [8, 7, 8])]},
        {"name": "crown", "parent": "heart", "pivot": [0, 14, 0], "cubes": [_cube(uv, "crown", [-2, 14, -2], [4, 2, 4])]},
    ])


def maw_geo():
    """The Mulch Maw: a clay pot whose two leafy jaws close over what it is fed, with a ring of thorn teeth."""
    uv = MAW_UV
    return _geo("mulch_maw", [
        {"name": "pot", "pivot": [0, 0, 0], "cubes": [_cube(uv, "pot", [-6, 0, -6], [12, 6, 12]),
                                                      _cube(uv, "tongue", [-2, 6, -3], [4, 1, 6])]},
        {"name": "jaw_north", "parent": "pot", "pivot": [0, 6, -6], "cubes": [
            _cube(uv, "jaw", [-6, 6, -6], [12, 5, 6]), _cube(uv, "tooth", [-4, 4, 0], [1, 2, 1]),
            _cube(uv, "tooth", [3, 4, 0], [1, 2, 1])]},
        {"name": "jaw_south", "parent": "pot", "pivot": [0, 6, 6], "cubes": [
            _cube(uv, "jaw", [-6, 6, 0], [12, 5, 6]), _cube(uv, "tooth", [-1, 4, -1], [1, 2, 1])]},
    ])


def gleaner_geo():
    """The Gleaner: a potted vine whose long arm, ending in a leafy hand, reaches out and back to harvest."""
    uv = GLEANER_UV
    return _geo("gleaner", [
        {"name": "pot", "pivot": [0, 0, 0], "cubes": [_cube(uv, "pot", [-4, 0, -4], [8, 6, 8]),
                                                      _cube(uv, "soil", [-3, 6, -3], [6, 1, 6])]},
        {"name": "stem", "parent": "pot", "pivot": [0, 6, 0], "cubes": [_cube(uv, "stem", [-1, 6, -1], [2, 6, 2])]},
        {"name": "arm", "parent": "stem", "pivot": [0, 12, 0], "cubes": [_cube(uv, "arm", [-1, 11, -1], [2, 2, 9])]},
        {"name": "hand", "parent": "arm", "pivot": [0, 12, 8], "cubes": [_cube(uv, "hand", [-2, 10.5, 8], [4, 3, 3])]},
    ], height=2)


def heart_animations():
    """Dormant: still. Beating: the heart swells and eases back, the leaves stir. Thriving beats twice as fast."""
    def beat(length):
        half = length / 2
        return {"loop": True, "animation_length": length, "bones": {
            "heart": {"scale": {"0.0": [1, 1, 1], str(half * 0.3): [1.08, 1.1, 1.08], str(half): [1, 1, 1]}},
            "leaves": {"rotation": {"0.0": [0, 0, 0], str(half): [0, 0, 4], str(length): [0, 0, 0]}}}}
    return {"format_version": "1.8.0", "animations": {
        "animation.verdant_heart.dormant": {"loop": True, "animation_length": 1.0, "bones": {
            "heart": {"scale": {"0.0": [1, 1, 1]}}}},
        "animation.verdant_heart.beating": beat(2.0),
        "animation.verdant_heart.thriving": beat(1.0),
    }}


def maw_animations():
    """Closed: jaws shut. Chewing: they open and close on what it eats."""
    return {"format_version": "1.8.0", "animations": {
        "animation.mulch_maw.closed": {"loop": True, "animation_length": 1.0, "bones": {
            "jaw_north": {"rotation": {"0.0": [0, 0, 0]}}, "jaw_south": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.mulch_maw.chewing": {"loop": True, "animation_length": 1.0, "bones": {
            "jaw_north": {"rotation": {"0.0": [0, 0, 0], "0.25": [-25, 0, 0], "0.5": [0, 0, 0]}},
            "jaw_south": {"rotation": {"0.0": [0, 0, 0], "0.25": [25, 0, 0], "0.5": [0, 0, 0]}}}},
    }}


def gleaner_animations():
    """Idle: the arm sways. Reaching: it swings out and back as it gathers."""
    return {"format_version": "1.8.0", "animations": {
        "animation.gleaner.idle": {"loop": True, "animation_length": 4.0, "bones": {
            "arm": {"rotation": {"0.0": [0, -10, 0], "2.0": [0, 10, 0], "4.0": [0, -10, 0]}}}},
        "animation.gleaner.reaching": {"loop": True, "animation_length": 2.0, "bones": {
            "arm": {"rotation": {"0.0": [0, 0, 0], "0.5": [20, 120, 0], "1.0": [20, 240, 0], "1.5": [10, 330, 0],
                                 "2.0": [0, 360, 0]}},
            "hand": {"rotation": {"0.0": [0, 0, 0], "1.0": [-30, 0, 0], "2.0": [0, 0, 0]}}}},
    }}


GEO = {"verdant_heart": heart_geo, "mulch_maw": maw_geo, "gleaner": gleaner_geo}
ANIMATIONS = {"verdant_heart": heart_animations, "mulch_maw": maw_animations, "gleaner": gleaner_animations}
SIZES = {"verdant_heart": (HEART_UV, HEART_SIZES), "mulch_maw": (MAW_UV, MAW_SIZES), "gleaner": (GLEANER_UV, GLEANER_SIZES)}
