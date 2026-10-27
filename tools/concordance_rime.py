"""Rime, the Rimekeepers' Principle (docs/features/arcane-concordance-rime-strata.md): cold, stillness and preservation.
Concordance Chunk B, unit 3.

- B-05, Rimekeeping: encountered by snow or ice examined; observed against warmth (something that holds fire,
  Hearthbinding's own specimens; either may come first, rules/ResearchEngine.java:21-22); understood at a Lampwright's
  Bench, by the winter survey (arriving in the Glacier Hall, or a Rime brew bottled by hand at an Alembic Crucible; gated
  by First Light only, so either route advances understanding, 02:136) or by notes; mastered by RIMEWORK_MASTERY kinds of
  cold work, each counted once (02:153). Preserve's work needs a crucible filled by hand, so the Alembic Arts too: no
  route to Rimekeeping's mastery skips the bench (or someone's notes), as the electives share one kind (B-P56).
- B-06, Rimebind (defense): Rimebound, a fifth off a creature's movement speed for 3 s, then a hidden thaw of 2 s in which
  no one's Rimebind takes it (02:78).
- B-07, Preserve (utility): an alteration in Rime's school holds an Alembic Crucible's heat in its band for 60 s (02:77).
- B-08, Frostward (unit 4): Winter Brine's status (the Rime alchemy property gives it); no frost gathers while it lasts
  and the Yeti King's chill does not take.
- B-10, the travel kit (unit 4, tools/concordance_rime_kit.py): a focus of water worn lengthens Preserve's hold
  (LESSER_FOCUS_HOLD_TICKS, FOCUS_HOLD_TICKS).
- B-10, the links (unit 4): a Frost boon arm's slow is elective cold work (B-P26, B-P53; the boon itself unchanged).
- B-09 (unit 5): the Yeti King's charge broken on an ice column is elective cold work for the player its column deed
  names (Rime.column), the same kind as a Frost boon arm's slow: one key between them (B-P56).

Java: concordance/rime (Rime, RimeSurvey, Rimebind, Frostward); tools/check_mod_data.py check_rime and (B-08)
check_winter_brine hold it to these tables.
Imports only leaf tables, never tools/concordance.py (tools/owner_art.py imports this module directly).
"""
from concordance_alchemy import BANDS, FROSTWARD
from concordance_ember import EMBER_SPECIMEN_TAG

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------ B-05 research
RESEARCH_ID = rid("rimekeeping")
RIME_SPECIMEN_TAG = f"{MOD}:rime_specimens"
# Snow and ice, all had in the Overworld. Powder snow is left out: using its bucket places it.
RIME_SPECIMENS = {"minecraft:snowball": "Snowball", "minecraft:snow_block": "Snow Block", "minecraft:ice": "Ice",
                  "minecraft:packed_ice": "Packed Ice", "minecraft:blue_ice": "Blue Ice"}
WARM_TAG = EMBER_SPECIMEN_TAG  # observed against warmth
# Rimekeeping's field and workshop study (Java: rime/RimeSurvey), gated by First Light understood.
SURVEY = rid("winter_survey")
SURVEY_KEYS = {"glacier_hall": "arrive in the Glacier Hall",
               "crucible": "bottle a brew that carries Rime at an Alembic Crucible"}
# Rimekeeping's mastery (Java: rime/Rime.ACTIVITY), gated by Rimekeeping understood.
# key -> (what else it needs past Rimekeeping, its invocations and a crucible: None for nothing, a research one player
# reaches alone, needed understood, or, for an elective, words; words). Preserve counts only a crucible holding a mixture
# (Rime.preserve), and only someone who understands the Alembic Arts puts water or anything else in one by hand
# (CrucibleBlockEntity.use), so both Preserve keys need them; the progression graph's rimework route asks for them too
# (tools/concordance_progression.py). A key whose need is words (elective()) is an elective: it can stand for one kind,
# and mastery never needs one (check_rime counts the others).
# Unit 5 (B-09) added the Yeti King's charge broken on a column (Rime.column) to the elective, which it shares with the
# Frost boon's slow (B-P56): one key, so the electives stand for at most one kind and every mastery keeps a Preserve key.
ALEMBIC_ARTS = rid("alembic_arts")
RIMEWORK = rid("rimework")
RIMEWORK_KEYS = {
    "preserve:cold": (ALEMBIC_ARTS, "Preserve a crucible holding a mixture in the cold band"),
    "preserve:heated": (ALEMBIC_ARTS, "Preserve a crucible holding a mixture in the warm, hot or searing band"),
    "restraint": (None, "Rimebind a hostile creature"),
    # The elective, one kind however it is done (B-P56; Java: Rime.KEY_ELECTIVE). Unit 4 (B-P26, B-P52, B-P53): a Frost
    # boon arm's slow that takes on a hostile creature (weapons/ArmItem.boon -> Rime.frostBoon). The boon is Arms VII's
    # own (tools/arms_variants.py FROST), unchanged (B-P10, B-P28); check_winter_links holds the trophies named here to
    # tools/arms_variants.py. Unit 5 (B-09, B-P16; 02:137): the Yeti King's charge broken on an ice column, for the
    # player its column deed names (lair/yeti/YetiKingEntity.baited -> Rime.column).
    "elective": ("a Frost boon arm, or a part in the Yeti King's fight",
                 "slow a hostile creature with a Frost boon arm (such as the Yeti King's Glacier Maul or Rimeclaw), or "
                 "draw the Yeti King's charge into an ice column, having already taken part in his fight (the two "
                 "count as one kind)"),
}
RIMEWORK_MASTERY = 3


def elective(needs):
    """Whether a kind of cold work needing `needs` is an elective: what it needs is words (a thing at hand, another
    player), not a research id. It can stand for one kind; mastery never needs one."""
    return needs is not None and ":" not in needs


_ELECTIVES = [text for needs, text in RIMEWORK_KEYS.values() if elective(needs)]
_ELECTIVE = "; ".join(_ELECTIVES)
_UNDERSTOOD = {"research": RESEARCH_ID, "state": "understood"}

RESEARCH = {"rimekeeping": {
    "name": "Rimekeeping", "principle": "rime", "tradition": "rimekeepers", "stage": "practitioner",
    "icon": "minecraft:packed_ice",
    "requires": [{"research": rid("first_light"), "state": "understood"}],
    "states": {
        "encountered": [{"type": "examine", "specimens": f"#{RIME_SPECIMEN_TAG}"}],
        "observed": [{"type": "examine", "specimens": f"#{WARM_TAG}"}],
        "understood": [{"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{RIME_SPECIMEN_TAG}"},
                       {"type": "practice", "activity": SURVEY},
                       {"type": "notes"}],
        "mastered": [{"type": "practice", "activity": RIMEWORK, "distinct": RIMEWORK_MASTERY}],
    },
    "unlocks": {"understood": {"invocations": [rid("rimebind"), rid("preserve")]}},
    "advancements": {
        "encountered": ("First Frost", "Examine snow or ice", "task", "minecraft:snowball"),
        "observed": ("Cold Against Warmth", "Examine snow or ice, and something that holds fire", "task", "minecraft:ice"),
        "understood": ("Rimekeeping", "Understand Rimekeeping: study snow or ice at a Lampwright's Bench, arrive in the "
                       "Glacier Hall, or bottle a brew that carries Rime", "goal", "minecraft:packed_ice"),
        "mastered": ("Rimekeeper", "Do three kinds of cold work: Preserve a crucible holding a mixture while it is cold, "
                     "and one while it is warm or hotter, and Rimebind a hostile creature (slowing a hostile creature "
                     "with a Frost boon arm, or drawing the Yeti King's charge into an ice column, can stand for one)",
                     "challenge", "minecraft:blue_ice"),
    },
    "locked": {"encountered": "Examine snow or ice first",
               "observed": "Examine snow or ice, and something that holds fire, first",
               "understood": "Understand Rimekeeping first", "mastered": "Master Rimekeeping first"},
}}

# --------------------------------------------------------------------------------------------- B-06 Rimebind
# Rimebound (Java: rime/Rimebind): a fifth off movement speed a level (ADD_MULTIPLIED_TOTAL) for RIMEBOUND_TICKS. Every
# binding that takes starts the hidden Thaw for its own time plus RIME_GRACE_TICKS; the boundary refuses Rimebound while
# a creature thaws (a Guard), so one creature is bound at most 3 s in 5 however many cast (02:78). Bosses in
# #jugcraft:concordance/resistant are bound half as long (Tolerance), so they thaw sooner.
RIMEBOUND = rid("rimebound")
RIME_THAW = rid("rime_thaw")
RIMEBOUND_SLOW = 0.2
RIMEBOUND_TICKS = 60
RIME_GRACE_TICKS = 40
RIMEBOUND_COLOUR = 0x8FD3F0
RIME_THAW_COLOUR = 0xCFEFF7
RIMEBIND_RANGE = 16  # the ray's

# --------------------------------------------------------------------------------------------- B-07 Preserve
# Java: Rime.HOLD_TICKS. 02:77's 60 s. The heat still moves inside its band; a recast restarts the minute, never adds.
# Not scaled by Spell Power (alterations take no duration, RulesParser.java:353-354). The crucible accepts holds up to the
# boundary's longest time (Rime.MAX_HOLD_TICKS = EffectSpec.MAX_DURATION), so unit 4's water foci (B-P24) fit unchanged.
PRESERVE_HOLD_TICKS = 1200
# Unit 4 (B-10, B-P24, B-P50): Preserve's hold with a Lesser Focus of Water or a Focus of Water worn (Java: RimeGear.holdTicks).
# Beyond the Initiate's Wand's 1200: the hold is the crucible's own state, not a composed effect's duration; the crucible
# cuts any hold at Rime.MAX_HOLD_TICKS (EffectSpec.MAX_DURATION, 2400). tools/concordance_rime_kit.py names the foci.
LESSER_FOCUS_HOLD_TICKS = 1800
FOCUS_HOLD_TICKS = 2400
_FOCI = (f"{PRESERVE_HOLD_TICKS // 20} seconds ({LESSER_FOCUS_HOLD_TICKS // 20} with a Lesser Focus of Water worn, "
         f"{FOCUS_HOLD_TICKS // 20} with a Focus of Water)")
PRESERVABLE = [rid("crucible")]  # Java: Rime.PRESERVABLE, #jugcraft:concordance/preservable
PRESERVE_WORD = "rimehold"

# ---------------------------------------------------------------------------------------------- B-08 Frostward
# Winter Brine's status (Java: rime/Frostward; tools/concordance_alchemy.py PROPERTIES["rime"] gives it). While it lasts a
# creature is never frozen through: its own effect tick clears what it has (the Yeti Mitten's check, done by the creature
# under it; powder snow adds one tick of frost after it), and the Yeti King's chill does not take (YetiKingEntity.chill).
# Timed; the Mitten stays the lasting answer (B-D3).
FROSTWARD_COLOUR = 0x73DAD8  # the ice of the owner's icon (frozen.png)

COMPONENTS = {
    "rimegrip": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 2,
                 "operation": {"effect": "status", "intent": "harmful", "principle": "rime", "magnitude": 0,
                               "duration": RIMEBOUND_TICKS, "status": RIMEBOUND, "stacking": "exclusive"},
                 "name": "Rimegrip",
                 "text": "Rimebind's frost: a creature moves a fifth slower for 3 seconds (Rimebound), then thaws for 2 "
                         "before frost can take it again."},
    PRESERVE_WORD: {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 2,
                    "operation": {"effect": "alteration", "intent": "helpful", "principle": "rime",
                                  "school": "spell_power:frost"},
                    "name": "Rimehold",
                    "text": f"Preserve's stillness: an Alembic Crucible's heat stays in its band for {_FOCI}."},
}

INVOCATIONS = {
    "rimebind": {
        "name": "Rimebind", "role": "defense", "composition": "ray struck rimegrip", "stage": "understood",
        "focus": 4, "mastered_focus": 3, "tunings": ["extend"], "work": 2,
        "persists": RIMEBOUND_TICKS + RIME_GRACE_TICKS, "cast": 0.5, "cooldown": 2.0,
        "animation": ("lance_cast", "lance_release"), "sounds": ("concordance.rime_gather", "concordance.rimebind"),
        "particle": "minecraft:snowflake", "icon": "minecraft:snowball",
        "description": f"Frost grips the first creature your ray meets, up to {RIMEBIND_RANGE} blocks away: it moves a "
                       f"fifth slower for {RIMEBOUND_TICKS // 20} seconds, then thaws, and no one's Rimebind takes it "
                       f"again for {RIME_GRACE_TICKS // 20} seconds.",
        "counter": "It slows and never roots: a bound creature keeps four fifths of its speed, and after every binding it "
                   "thaws, whoever casts next. Milk clears it; the Yeti King and the Cinder Tyrant are bound half as long; "
                   "the Ender Dragon, the Wither, the Warden and the Elder Guardian shrug it off.",
        "fizzle": "The frost finds nothing to bind: Rimebind needs a creature within %s blocks that is not thawing",
    },
    "preserve": {
        "name": "Preserve", "role": "utility", "composition": f"touch struck {PRESERVE_WORD}", "stage": "understood",
        "focus": 3, "mastered_focus": 2, "tunings": [], "work": 4, "persists": PRESERVE_HOLD_TICKS,
        "cast": 0.6, "cooldown": 3.0,
        "animation": ("kindle_cast", "kindle_release"), "sounds": ("concordance.rime_gather", "concordance.preserve"),
        "particle": "minecraft:snowflake", "icon": rid("crucible"),
        "description": f"Stills the Alembic Crucible you touch: for {_FOCI} its heat stays in the band it is in (cold, "
                       "warm, hot or searing), whatever burns beneath it.",
        "counter": "It holds one crucible you could change by hand, for a minute (two at most, with a Focus of Water worn): "
                   "the heat still moves inside its band, "
                   "a second cast only starts the hold again, and it keeps nothing else (no chest, machine, food or "
                   "item).",
        "fizzle": "Nothing to preserve: Preserve holds an Alembic Crucible within %s blocks",
    },
}
for _invocation in INVOCATIONS.values():
    _invocation.update({"principle": "rime", "research": RESEARCH_ID})

STATUS_NAMES = {RIMEBOUND: "Rimebound"}
MESSAGES = {
    "examine.cold": "The %s is cold enough to ache. You hold it and watch how slowly it gives",
    "crucible.status_held": "Alembic Crucible: %s, %s degrees, %s parts; preserved for %s s",
}
TOOLTIPS = {"jade.held": "Preserved: its band held %s s more"}
SOUND_EVENTS = {"concordance.rime_gather": "Frost gathers", "concordance.rimebind": "Frost grips",
                "concordance.preserve": "Cold settles"}
SOUND_FILES = {}
# The owner's files, copied byte for byte by tools/owner_art.py (MAGIC_ASSETS.md): runtime path -> path under Magic/.
OWNER_FILES = {
    "textures/spell/rimebind.png": "assets/ars_jimbaux/textures/item/freeze.png",
    "textures/spell/preserve.png": "assets/ars_jimbaux/textures/item/extend_time.png",
    "textures/mob_effect/rimebound.png": "assets/ars_jimbaux/textures/mob_effect/freezing.png",
    "textures/mob_effect/rime_thaw.png": "assets/ars_nouveau/textures/mob_effect/soaked.png",
    # B-08 (unit 4, B-P43): the owner's "Frozen" icon, used for its opposite, the ward against frost.
    "textures/mob_effect/frostward.png": "assets/ars_jymbaumental/textures/mob_effect/frozen.png",
}


def lang_entries(lang):
    lang[f"tag.item.{MOD}.rime_specimens"] = "Snow and Ice"
    lang[f"effect.{MOD}.rimebound"] = "Rimebound"
    lang[f"effect.{MOD}.rime_thaw"] = "Thawing"
    lang[f"effect.{FROSTWARD.replace(':', '.')}"] = "Frostward"  # B-08


def _band_floors():
    return ", ".join(f"{name} {'below ' + str(BANDS['warm'][0]) if lo is None else 'from ' + str(lo)}"
                     for name, (lo, _) in BANDS.items())


def codex():
    research = "rimekeeping"
    specimens = "\\\n".join(f"- {name}" for name in RIME_SPECIMENS.values())
    work = "; ".join(text for needs, text in RIMEWORK_KEYS.values() if not elective(needs))
    return {
        ("winter", "rimekeeping"): {
            "name": "Rimekeeping", "x": 0, "y": 0, "icon": "minecraft:packed_ice", "condition": None,
            "description": "Rime: research",
            "pages": [
                ("text", "Rimekeeping", "Once you understand First Light, learn cold as the Rimekeepers do: what it keeps, "
                 "and how slowly. **Sneak and use** snow or ice to examine it (sneaking, a snowball is examined, not "
                 "thrown):\\\n" + specimens),
                ("text", "Cold Against Warmth", "Cold is known against warmth: examine something that holds fire too "
                 "(coal, charcoal, a torch, a campfire). Either may come first.", (research, "encountered")),
                ("text", "Observed", "Understand Rimekeeping by any one of: study snow or ice at a **Lampwright's Bench**; "
                 "arrive in the **Glacier Hall**; bottle a brew that carries Rime at an **Alembic Crucible** (the Alembic "
                 "Arts understood); or read someone's **Research Notes**.", (research, "observed")),
                # Unconditioned: check_progression wants a readable recipe page for every reachable recipe-made item.
                ("crafting_recipe", "Frost Horn", "Blown at night on snow or ice in the Overworld, it takes you down into "
                 "the Glacier Hall. Arriving there is a study of cold in itself.", rid("frost_horn")),
                ("text", "Understood", f"You can now cast **Rimebind** and **Preserve** with any Concordance instrument. "
                 f"To **master** Rimekeeping, do {RIMEWORK_MASTERY} kinds of cold work: {work}."
                 + (f" Any of these can stand for one: {_ELECTIVE}." if len(_ELECTIVES) > 1
                    else f" This can stand for one of them: {_ELECTIVE}." if _ELECTIVES else "")
                 + " Only someone who understands the **Alembic Arts** can put water in a crucible by hand. A magma block "
                 "keeps a crucible warm and a campfire makes it hot.", (research, "understood")),
                ("text", "Mastered", "Rimebind and Preserve cost a point less Focus.", (research, "mastered")),
            ]},
        ("winter", "rimebound"): {
            "name": "Rimebound", "x": 2, "y": 0, "icon": "minecraft:snowball", "condition": (research, "understood"),
            "description": "Frost that slows and lets go",
            "pages": [("text", "Rimebound",
                       "A rimebound creature moves a fifth slower for 3 seconds. Then it thaws for 2: nobody's Rimebind "
                       "takes it again until then, so no creature is held more than 3 seconds in 5, however many "
                       "Rimekeepers cast. The Yeti King and the Cinder Tyrant are bound half as long; the Ender Dragon, the "
                       "Wither, the Warden and the Elder Guardian shrug it off. Milk clears it. It reaches only what you "
                       "could strike by hand, and another player only where the server allows it and they are not in your "
                       "party."),
                      ("text", "Against the Yeti King", "The Yeti King moves by his own steps, so the slow does not "
                       "hold him. Bound, he holds his wind-up (the one under way unless it is about to strike, else his "
                       "next) a moment longer, frost-locked, before it strikes: once each time he thaws. Drawing his "
                       "charge into an ice column, as the player it was aimed at, having already taken part in his "
                       "fight, is cold work too, once you understand Rimekeeping: the same kind as a Frost boon arm's "
                       "slow.")]},
        ("winter", "preserving"): {
            "name": "Preserve", "x": 4, "y": 0, "icon": rid("crucible"), "condition": (research, "understood"),
            "description": "Holding a crucible's heat",
            "pages": [
                ("text", "Preserve", f"Preserve stills an **Alembic Crucible** you touch: for "
                 f"{_FOCI} its heat stays in the band it is in ({_band_floors()} degrees), "
                 "whatever burns or goes out beneath it. The heat still moves inside the band, a degree a tick."),
                ("text", "What it will not hold", "Only a crucible you could change by hand: claims, protected towns and "
                 "spawn protection refuse it. Never a chest, barrel, machine, food or item. A second cast starts the "
                 "hold again; it never adds to it. Jade, and the crucible's reading when you use it empty-handed, show "
                 "the seconds left."),
            ]},
    }


CATEGORY = {"winter": {"name": "Winter", "icon": "minecraft:packed_ice", "sort": 17,
                       "description": "The Rimekeepers' practice: cold kept and used"}}


def tags(tags):
    for ref in RIME_SPECIMENS:
        tags.add("item", RIME_SPECIMEN_TAG, ref)
    for block in PRESERVABLE:
        tags.add("block", f"{MOD}:concordance/preservable", block)


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)


def write_data(write, data):
    pass
