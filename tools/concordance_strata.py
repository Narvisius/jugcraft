"""Strata, the Stratawrights' Principle (docs/features/arcane-concordance-rime-strata.md): stone, structure and weight.
Concordance Chunk B, unit 3. B-05, Stratawrighting: read from stone and raw Thallite; its specimens sit inside Assay's raw
matter (#jugcraft:assay_specimens holds #jugcraft:strata_specimens), so one examination serves both (09:13), and vanilla
stone alone completes it (the thallite switch off, 02:140). B-06, Brace (support): Braced, half the knockback of the next
push, for 5 s, on the caster and their party (02:79). Java: concordance/strata (Strata, Brace)."""
from concordance_equivalence import ASSAY_SPECIMEN_TAG

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


RESEARCH_ID = rid("stratawrighting")
STRATA_SPECIMEN_TAG = f"{MOD}:strata_specimens"
STRATA_SPECIMENS = {"minecraft:granite": "Granite", "minecraft:diorite": "Diorite", "minecraft:andesite": "Andesite",
                    "minecraft:tuff": "Tuff", rid("raw_thallite"): "Raw Thallite"}
# Bridging blocks: sneak-use in the air examines a specimen (Examination.java:53-63), so these are never one.
NOT_SPECIMENS = ["minecraft:cobblestone", "minecraft:stone", "minecraft:cobbled_deepslate", "minecraft:deepslate"]
OBSERVED_DISTINCT = 3
ANCHORING = rid("anchoring")  # Java: strata/Strata.ACTIVITY
ANCHORING_KEYS = {
    "ground:living": (None, "Brace someone standing on soil (grass, dirt, mud, moss)"),
    "ground:earthen": (None, "Brace someone standing on bare stone, sand or gravel"),
    "held": (None, "have a Brace you cast hold against a blow"),
    "ally": ("another player", "brace a party member"),
    "foundation": ("Circle Lore", "complete a ritual at a Circle Anchor"),
    "rooted": ("Earthbound Thallite", "take a blow in Earthbound Thallite armour on natural ground"),
}
ANCHORING_MASTERY = 3
# Concordance Chunk C, C-04: Root and Rivet's codex entry (tools/concordance_rituals.py), linked from Understood. A link to
# no entry stops the whole book opening, so check_concordance holds every entry:// link to an entry on disk (C-P42).
ROOT_AND_RIVET = "[Root and Rivet](entry://jugcraft:arcane_concordance/circles/root_and_rivet)"
_UNDERSTOOD = {"research": RESEARCH_ID, "state": "understood"}
_ELECTIVE = "; ".join(text for needs, text in ANCHORING_KEYS.values() if needs is not None)

RESEARCH = {"stratawrighting": {
    "name": "Stratawrighting", "principle": "strata", "tradition": "stratawrights", "stage": "practitioner",
    "icon": "minecraft:tuff",
    "requires": [{"research": rid("first_light"), "state": "understood"}],
    "states": {
        "encountered": [{"type": "examine", "specimens": f"#{STRATA_SPECIMEN_TAG}"}],
        "observed": [{"type": "examine", "specimens": f"#{STRATA_SPECIMEN_TAG}", "distinct": OBSERVED_DISTINCT}],
        "understood": [{"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{STRATA_SPECIMEN_TAG}"},
                       {"type": "notes"}],
        "mastered": [{"type": "practice", "activity": ANCHORING, "distinct": ANCHORING_MASTERY}],
    },
    "unlocks": {"understood": {"invocations": [rid("brace")]}},
    "advancements": {
        "encountered": ("Read the Stone", "Examine ordinary stone or raw Thallite", "task", "minecraft:granite"),
        "observed": ("Strata", f"Examine {OBSERVED_DISTINCT} different kinds of stone", "task", "minecraft:tuff"),
        "understood": ("Stratawrighting", "Understand Stratawrighting: study stone at a Lampwright's Bench", "goal",
                       "minecraft:polished_andesite"),
        "mastered": ("Stratawright", "Anchor three ways: Brace someone on soil, Brace someone on bare stone, sand or "
                     "gravel, and have a Brace you cast hold against a blow (bracing a party member, a ritual completed "
                     "or a blow taken in Earthbound Thallite can stand for one)", "challenge", "minecraft:lodestone"),
    },
    "locked": {"encountered": "Examine ordinary stone or raw Thallite first",
               "observed": f"Examine {OBSERVED_DISTINCT} different kinds of stone first",
               "understood": "Understand Stratawrighting first", "mastered": "Master Stratawrighting first"},
}}

# ----------------------------------------------------------------------------------------------- B-06 Brace
# Braced (Java: strata/Brace): +BRACE_RESISTANCE knockback resistance for BRACE_TICKS, exclusive; spent by the first push
# it meets (a blow that knocks back, a harmful Concordance push, or a push that asks for it: the Yeti King's every push
# (B-09), the Leaf Blower's stream) and gone at the end of that server tick (at the start of the next, if spent between
# ticks). Helpful pushes (one's own Flashstep) neither spend it nor feel it (B-P12). Accepted exception: vanilla's
# knockback that comes without a damaging blow is braced but does not spend it (B-P30).
BRACED = rid("braced")
BRACE_TICKS = 100
BRACE_RESISTANCE = 0.5
BRACED_COLOUR = 0x8A7F72

COMPONENTS = {
    "bracing": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 1,
                "operation": {"effect": "status", "intent": "helpful", "principle": "strata", "magnitude": 0,
                              "duration": BRACE_TICKS, "status": BRACED, "stacking": "exclusive"},
                "name": "Bracing", "text": "Brace's stance: the next push moves its wearer half as far (Braced). One blow "
                                          "spends it."},
}
INVOCATIONS = {
    "brace": {
        "name": "Brace", "role": "support", "composition": "here allies bracing", "stage": "understood",
        "focus": 3, "mastered_focus": 2, "tunings": ["widen"], "work": 4, "persists": BRACE_TICKS,
        "cast": 0.4, "cooldown": 8.0,
        "animation": ("aegis_cast", "aegis_release"), "sounds": ("concordance.strata_gather", "concordance.brace"),
        "particle": "minecraft:white_ash", "icon": "minecraft:cobbled_deepslate",
        "description": "Sets you and the members of your party within 4 blocks like stone: for 5 seconds, the next blow "
                       "or push moves each of you half as far.",
        "counter": "One blow spends it; it never stacks and never lessens harm; your own Flashstep and a friend's helpful "
                   "push ignore it; a teleport, a portal or a lair's mist moves the braced as anyone. It is not the war "
                   "fork's Brace (harder blows against a charging foe) nor a tower shield's steadiness.",
        "fizzle": "Everyone near is braced already",
    },
}
for _invocation in INVOCATIONS.values():
    _invocation.update({"principle": "strata", "research": RESEARCH_ID})

STATUS_NAMES = {BRACED: "Braced"}
MESSAGES = {"examine.weight": "You heft the %s and feel how it bears its weight"}
SOUND_EVENTS = {"concordance.strata_gather": "Stone stirs", "concordance.brace": "Stone braces"}
# Brace's release is the owner's four earth recordings, as their gaia_family event plays them (ars_jimbaux/sounds.json).
SOUND_FILES = {"concordance.brace": [f"concordance/gaia_{n}" for n in range(1, 5)]}
OWNER_FILES = {
    "textures/spell/brace.png": "assets/ars_jimbaux/textures/item/gravity.png",
    "textures/mob_effect/braced.png": "assets/ars_jimbaux/textures/mob_effect/gravity.png",
    **{f"sounds/{name}.ogg": f"assets/ars_jimbaux/sounds/{name.split('/')[1]}.ogg"
       for names in SOUND_FILES.values() for name in names},
}


def lang_entries(lang):
    lang[f"tag.item.{MOD}.strata_specimens"] = "Stone and Thallite"
    lang[f"effect.{MOD}.braced"] = "Braced"


def codex():
    research = "stratawrighting"
    specimens = "\\\n".join(f"- {name}" for name in STRATA_SPECIMENS.values())
    own = "; ".join(text for needs, text in ANCHORING_KEYS.values() if needs is None)
    return {
        ("stone", "stratawrighting"): {
            "name": "Stratawrighting", "x": 0, "y": 0, "icon": "minecraft:tuff", "condition": None,
            "description": "Strata: research",
            "pages": [
                ("text", "Stratawrighting", "Once you understand First Light, read stone as the Stratawrights do: how it "
                 "bears weight. **Sneak and use** one of these to examine it:\\\n" + specimens + "\\\n\\\nIt is raw "
                 "matter too: the same examination begins the Balancewrights' **Assay**. Cobblestone, stone and deepslate "
                 "are never examined, so you can bridge with them."),
                ("text", "Observed", f"You have examined {OBSERVED_DISTINCT} kinds of stone. Study one at a "
                 "**Lampwright's Bench**, or read someone's **Research Notes**.", (research, "observed")),
                ("text", "Understood", f"You can now cast **Brace**. To **master** Stratawrighting, anchor "
                 f"{ANCHORING_MASTERY} ways: {own}. Any of these can stand for one: {_ELECTIVE}. {ROOT_AND_RIVET}, a "
                 "Circlewrights' rite that roots a Thallite plate into a foundation, is one such ritual.",
                 (research, "understood")),
                ("text", "Mastered", "Brace costs a point less Focus.", (research, "mastered")),
            ]},
        ("stone", "braced"): {
            "name": "Braced", "x": 2, "y": 0, "icon": "minecraft:cobbled_deepslate",
            "condition": (research, "understood"), "description": "Standing like stone",
            "pages": [("text", "Braced",
                       "Brace braces you and the members of your party within 4 blocks for 5 seconds: the next blow, or "
                       "anyone's harmful push (a Leaf Blower's stream too), moves you half as far and spends it. A shove "
                       "that does you no harm may be halved without spending it. A second Brace while one holds does "
                       "nothing. Your own Flashstep ignores it. It lessens no harm, and nothing holds you against a "
                       "lair's mist, a portal or a teleport. Not the war fork's Brace, which strikes a charging foe harder."),
                      ("text", "Against the Yeti King", "No knockback resistance lessens the Yeti King's throws: not "
                       "armour's, not Earthbound Thallite's (his boulders and icicles knock you back as anything does, "
                       "and a shield that wholly blocks a swipe or spike stops its throw). Braced, the next of his "
                       "blows, slams, charges or spikes throws you half as far. A Brace you put on a friend that takes "
                       "one of his throws is your part in his fight.")]},
    }


CATEGORY = {"stone": {"name": "Stone", "icon": "minecraft:tuff", "sort": 18,
                      "description": "The Stratawrights' practice: stone read and leant on"}}


def tags(tags):
    for ref in STRATA_SPECIMENS:
        tags.add("item", STRATA_SPECIMEN_TAG, ref)
    tags.add("item", ASSAY_SPECIMEN_TAG, f"#{STRATA_SPECIMEN_TAG}")  # one examination of raw matter (09:13)


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)


def write_data(write, data):
    pass
