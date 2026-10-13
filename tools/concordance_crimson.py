"""Roadmap step 16: Crimson resources and living equipment (docs/features/arcane-concordance-vitae.md).

The Crimson Vigil's practice. Three things are kept apart: health (vanilla's, healed by anything), Vitae (what an
offering turns health into, held in a Crimson Chalice) and offering exhaustion (what offering leaves, cleared only by
time). An offering is the only way health becomes Vitae: it never gives more Vitae than the health it takes, never
leaves the giver below a floor, and each one adds exhaustion, which lowers what the next yields and, at its limit,
refuses offerings; no food, potion, spell or regeneration touches exhaustion, so healing never resets the loop.

Vitae buys a Crimson Surge (Focus in an emergency, once a minute) and vigor for the Thornheart Blade, living equipment
that develops through slaying, enduring and being nourished, with diminishing returns for repeating one deed.

The rite is data (data/jugcraft/concordance/offering, read by Java's concordance/crimson). tools/concordance.py merges
these tables into its own; numbers the Java repeats are checked by tools/check_mod_data.py.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Exhaustion (Java: crimson/Exhaustion.java) and efficiency (crimson/Offerings.java: percent by exhaustion band).
EXHAUSTION_MAX = 12
RECOVERY_TICKS = 2400  # one point every two minutes, and nothing else clears it
EFFICIENCY = [(3, 100), (7, 50), (11, 25)]  # up to this many points: this percent; at the limit, none
# The rite parser's limits (Java: crimson/CrimsonParser.java).
MAX_HEALTH = 20
MAX_COOLDOWN = 24000
# Living growth (Java: crimson/Growth.java).
WINDOW_TICKS = 24000  # a day of game time
MAX_SUBJECTS = 32
DAILY_CAP = 24
DIMINISHING = [4, 2, 1, 1]
MAX_VIGOR = 64
VIGOR_PER_VITAE = 4
STAGES = [(0, 0, 0), (20, 5, 2), (60, 10, 3), (150, 30, 3)]  # (total, each, kinds reaching "each")
STAGE_NAMES = ["Dormant", "Awakened", "Grown", "Flourishing"]
# The Vigil (Java: vigil/Vigil.java).
CHALICE_CAPACITY = 32
SURGE_VITAE = 6
SURGE_FOCUS = 6
SURGE_COOLDOWN = 1200
NOURISH_VITAE = 2
BLOW_VIGOR = 1
STAGE_DAMAGE = 1

CRIMSON_SPECIMEN_TAG = f"{MOD}:crimson_specimens"
CRIMSON_SPECIMENS = {
    "minecraft:sweet_berries": "Sweet Berries", "minecraft:crimson_roots": "Crimson Roots",
    "minecraft:crimson_fungus": "Crimson Fungus", "minecraft:nether_wart": "Nether Wart", "minecraft:spider_eye": "Spider Eye",
}
LIVING_GROWTH = rid("living_growth")  # the practice a blade's stages record (Java: Vigil.ACTIVITY)

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "crimson_rites": {
        "name": "Crimson Rites",
        "principle": "hollow",
        "tradition": "crimson_vigil",
        "stage": "practitioner",
        "icon": rid("crimson_chalice"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{CRIMSON_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{CRIMSON_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{CRIMSON_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: a Thornheart Blade you wield grown to its second stage.
            "mastered": [{"type": "practice", "activity": LIVING_GROWTH, "distinct": 2}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Red Thread", "Examine a living thing that bleeds or stings", "task", "minecraft:sweet_berries"),
            "observed": ("Vigil Notes", "Examine three different crimson specimens", "task", "minecraft:crimson_roots"),
            "understood": ("Crimson Rites", "Understand Crimson Rites: study a crimson specimen at a Lampwright's Bench",
                           "goal", rid("crimson_chalice")),
            "mastered": ("Thornheart", "Grow a Thornheart Blade to its second stage", "challenge", rid("thornheart_blade")),
        },
        "locked": {"encountered": "Examine a crimson specimen first",
                   "observed": "Examine three different crimson specimens first",
                   "understood": "Understand Crimson Rites first", "mastered": "Master Crimson Rites first"},
    },
}

# ------------------------------------------------------------------------------------------------- rites

# "health" is in health points (half-hearts); "floor" the least health an offering leaves; "cooldown" game ticks.
RITES = {
    "offering": {"health": 4, "vitae": 4, "exhaustion": 3, "floor": 8, "cooldown": 100},
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "crimson_chalice": {"name": "Crimson Chalice",
                        "tooltip": "Use: offer health for Vitae. Sneak: a Crimson Surge. With a Thornheart Blade in the "
                                   "other hand: feed it."},
    "thornheart_blade": {"name": "Thornheart Blade",
                         "tooltip": "Living: it grows by slaying, enduring and being fed, never by one deed repeated."},
}

# ------------------------------------------------------------------------------------------------- text

MESSAGES = {
    "vigil.unknown": "You have not understood Crimson Rites (the codex says how)",
    "vigil.offered": "Offered %s health for %s Vitae: %s / %s",
    "vigil.too_weak": "Too weak: an offering must leave you %s health",
    "vigil.exhausted": "Too exhausted to offer: clear in %s seconds (healing does not help)",
    "vigil.too_soon": "Too soon after your last offering",
    "vigil.full": "The chalice is full",
    "vigil.surged": "Crimson Surge: %s Vitae for %s Focus",
    "vigil.surge_too_soon": "A surge needs a minute to settle",
    "vigil.surge_too_little": "A surge takes %s Vitae",
    "vigil.surge_full": "Your Focus is already full",
    "vigil.disabled": "The Concordance is switched off on this server",
    "vigil.sated": "The blade is sated",
    "vigil.too_little": "Feeding the blade takes %s Vitae",
    "vigil.fed": "The blade drinks: vigor %s / %s",
    "vigil.grew": "Your Thornheart Blade grows: %s",
    "vigil.health": "Health: %s / %s",
    "vigil.vitae": "Vitae in the chalice: %s / %s",
    "vigil.exhaustion": "Offering exhaustion: %s / %s (offerings yield %s%%), clear in %s seconds",
    "vigil.blade": "Thornheart Blade: %s; slaying %s, enduring %s, nourished %s; vigor %s / %s",
}
for _stage, _name in enumerate(STAGE_NAMES):
    MESSAGES[f"vigil.stage.{_stage}"] = _name

SCREEN = {
    "vigil.health": "Health %s/%s",
    "vigil.vitae": "Vitae %s/%s",
    "vigil.exhaustion": "Exhaustion %s/%s, %s%%, clear %s:%s",
}

TOOLTIPS = {
    "crimson_specimen": "Crimson specimen: sneak and use to examine it",
    "chalice.vitae": "Vitae: %s / %s",
    "blade.deeds": "Slaying %s, enduring %s, nourished %s",
    "blade.vigor": "Vigor: %s / %s",
    "blade.asleep": "Asleep: no vigor (feed it Vitae)",
}


def lang_entries(lang):
    lang[f"tag.item.{CRIMSON_SPECIMEN_TAG.replace(':', '.')}"] = "Crimson Specimens"
    for key, text in SCREEN.items():
        lang[f"screen.{MOD}.concordance.{key}"] = text


# ------------------------------------------------------------------------------------------------- codex

def _efficiency_text():
    parts = []
    low = 0
    for top, percent in EFFICIENCY:
        parts.append(f"{low} to {top} points: {percent}%")
        low = top + 1
    return "; ".join(parts) + f"; at {EXHAUSTION_MAX} no offering at all"


def codex():
    art = "crimson_rites"
    rite = RITES["offering"]
    specimens = "\\\n".join(f"- {name}" for name in CRIMSON_SPECIMENS.values())
    return {
        ("vigil", "crimson_rites"): {
            "name": "Crimson Rites", "x": 0, "y": 0, "icon": rid("crimson_chalice"), "condition": None,
            "description": "Vitae: research",
            "pages": [
                ("text", "Crimson Rites",
                 "Once you understand First Light, look closely at living things that bleed or sting. **Sneak and use** "
                 "one of these to examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 "The Crimson Chalice answers you now. To **master** Crimson Rites, grow a Thornheart Blade you wield to "
                 "its second stage.", (art, "understood")),
                ("text", "Mastered", "You know what a life can give, and what it cannot.", (art, "mastered")),
            ],
        },
        ("vigil", "offerings"): {
            "name": "Offerings", "x": 2, "y": 0, "icon": rid("crimson_chalice"), "condition": (art, "encountered"),
            "description": "Health, Vitae and exhaustion",
            "pages": [
                ("crafting_recipe", "Crimson Chalice",
                 f"Use it to make an **offering**: {rite['health']} health for up to {rite['vitae']} **Vitae**, which "
                 f"the chalice holds (up to {CHALICE_CAPACITY}). An offering never leaves you below {rite['floor']} "
                 f"health, never comes within {rite['cooldown'] // 20} seconds of the last, and takes no health when "
                 f"the chalice is full.", rid("crimson_chalice")),
                ("text", "Three Things Apart",
                 "**Health** is yours to heal as ever. **Vitae** is what an offering made of it. **Exhaustion** is what "
                 f"offering leaves behind: each offering adds {rite['exhaustion']} points, and they clear one every "
                 f"{RECOVERY_TICKS // 1200} minutes and by nothing else: no food, potion, spell or regeneration. "
                 "Healing to full does not let you offer again as if fresh."),
                ("text", "Efficiency",
                 f"An offering's yield falls with your exhaustion: {_efficiency_text()}. Rounded down. So a careful "
                 "offering now and then gives far more than many in a row. **/jugcraft concordance vitae** says all "
                 "three, and how long until you are clear."),
            ],
        },
        ("vigil", "surge"): {
            "name": "Crimson Surge", "x": 4, "y": 0, "icon": "minecraft:redstone", "condition": (art, "understood"),
            "description": "Focus in an emergency",
            "pages": [
                ("text", "Crimson Surge",
                 f"**Sneak and use** the chalice: {SURGE_VITAE} Vitae become {SURGE_FOCUS} Focus at once, at most once "
                 f"a minute and never beyond your full Focus. An emergency measure: what you can offer in an hour is "
                 f"bounded by your exhaustion, so it never becomes a spring of Focus."),
            ],
        },
        ("vigil", "thornheart"): {
            "name": "Thornheart Blade", "x": 2, "y": 2, "icon": rid("thornheart_blade"), "condition": (art, "understood"),
            "description": "Living equipment",
            "pages": [
                ("crafting_recipe", "Thornheart Blade",
                 "A living sword. It grows by three kinds of deed: **slaying** hostile creatures, **enduring** harm while "
                 "you wield it, and being **nourished** (use the chalice with the blade in your other hand: "
                 f"{NOURISH_VITAE} Vitae for {NOURISH_VITAE * VIGOR_PER_VITAE} vigor).", rid("thornheart_blade")),
                ("text", "Varied Use",
                 f"Each deed pays by how often its kind of creature or harm has come up today: "
                 f"{', '.join(str(n) for n in DIMINISHING)}, then nothing; each kind of deed pays at most {DAILY_CAP} a "
                 f"day. A mob farm or a cactus stops paying at once. Its stages need several kinds: Awakened (20, two "
                 f"kinds with 5), Grown (60, every kind with 10), Flourishing (150, every kind with 30)."),
                ("text", "Vigor",
                 f"Each stage adds {STAGE_DAMAGE} attack damage while the blade has vigor; every blow on a living "
                 f"creature spends {BLOW_VIGOR}. Without vigor its powers sleep, but nothing it has grown is lost."),
            ],
        },
    }


CATEGORY = {"vigil": {"name": "Vigil", "icon": rid("crimson_chalice"), "sort": 8,
                      "description": "The Crimson Vigil's practice: offerings, Vitae and living equipment"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in CRIMSON_SPECIMENS:
        tags.add("item", CRIMSON_SPECIMEN_TAG, ref)


def offering_clip():
    """The offering gesture (Player Animation Library "emote" format, radians): the chalice hand lifts to the chest,
    the other presses the palm to it, a breath, and both lower. One second; plays once."""
    def move(tick, part, axis, value):
        return {"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, part: {axis: value}}
    moves = []
    for tick, right, left in ((0, -0.20, -0.10), (6, -1.35, -1.10), (14, -1.45, -1.25), (20, -0.25, -0.10)):
        moves += [move(tick, "rightArm", "pitch", right), move(tick, "rightArm", "yaw", -0.35),
                  move(tick, "leftArm", "pitch", left), move(tick, "leftArm", "yaw", 0.55)]
    moves += [move(0, "head", "pitch", 0.0), move(8, "head", "pitch", 0.30), move(20, "head", "pitch", 0.0)]
    return {"name": "vigil_offering", "author": "Jugcraft", "description": "Arcane Concordance: vigil_offering",
            "emote": {"isLoop": "false", "returnTick": 0, "beginTick": 0, "endTick": 20, "stopTick": 24, "degrees": False,
                      "moves": moves}}


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    write(assets / "models" / "item" / "crimson_chalice.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/crimson_chalice")}})
    write(assets / "models" / "item" / "thornheart_blade.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": rid("item/thornheart_blade")}})
    for item in ("crimson_chalice", "thornheart_blade"):
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    write(assets / "player_animations" / "vigil_offering.json", offering_clip())
    feature = "concordance"
    recipes = {
        "crimson_chalice": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["G G", "RBR", " G "],
                            "key": {"G": "minecraft:gold_ingot", "R": "minecraft:redstone", "B": "minecraft:glass_bottle"},
                            "result": {"id": rid("crimson_chalice"), "count": 1}},
        "thornheart_blade": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["BWB", " S ", " R "],
                             "key": {"B": "minecraft:sweet_berries", "W": "minecraft:nether_wart",
                                     "S": "minecraft:iron_sword", "R": "minecraft:redstone"},
                             "result": {"id": rid("thornheart_blade"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key, info in RITES.items():
        write(data / "concordance" / "offering" / f"{key}.json", dict({"schema": 1}, **info))
