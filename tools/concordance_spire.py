"""Roadmap step 25: the Concord Spire, a representative endgame project (docs/features/arcane-concordance-spire.md).

A spire is founded at a Spire Heart by someone at the Master stage who knows its configuration's research, then raised
through four phases round the heart: the Foundation (four Ley Pylons and four Warding Stones), the Shaft (three blocks of
dark stone on the heart, and the configuration's practice carried through twice), the Crown (the configuration's
crown, and the Kindling rite completed at a Lesser Circle nearby) and the Kindling (three days of upkeep held in a row).
Raised, it keeps working while it is whole, attended (the configuration's practice within a week) and supplied (each
day its configuration's item from the heart's store, which couriers fill, and Ley Charge from its own pylons). Its field
then lights the dark (Lantern), grows crops (Verdant) or restores Focus and reveals hostile creatures (Star) around it.
Progress is the world's, never the heart's; nothing is ever broken or replaced; and raising one records a milestone the
Architect stage reads.

WONDER and CONFIGURATIONS become data/jugcraft/concordance/wonder and wonder_configuration (Java: wonder/WonderParser,
Spires; spire/ConcordSpire); STRUCTURES and RITUALS join tools/concordance_rituals.py's.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# Java: wonder/WonderParser.java.
MAX_PHASES = 6
MAX_PRACTICES = 8
MAX_SUSTAIN = 7
MAX_ATTENDANCE = 28
MIN_PULSE = 20
MAX_PULSE = 1200
MAX_RITE_RANGE = 16
MAX_RADIUS = 32
MAX_COUNT = 8
MAX_UPKEEP = 64
MAX_LEY = 64
MAX_REQUIRES = 6
# Java: wonder/Spires.java, spire/ConcordSpire.java, spire/SpireHeartBlockEntity.java.
DAY = 24_000
REACH = 16
HOLD_TICKS = 400
CHECK_TICKS = 20
SLOTS = 9
WONDER_ID = rid("concord_spire")
MILESTONE = rid("spire_raised")
FIELD_KINDS = ["illumination", "growth", "focus"]

SPIRE_STONE_TAG = f"{MOD}:concordance/spire_stone"
SPIRE_STONES = ["minecraft:polished_deepslate", "minecraft:deepslate_bricks", "minecraft:deepslate_tiles",
                "minecraft:chiseled_deepslate"]

WONDER = {
    "heart": rid("spire_heart"), "stage": "master", "rite": rid("spire_kindling"), "rite_range": 12, "attendance_days": 7,
    "pulse": 100,
    "phases": [
        {"id": "foundation", "structure": rid("spire_foundation")},
        {"id": "shaft", "structure": rid("spire_shaft"), "practices": 2},
        {"id": "crown", "structure": "crown", "rite": True},
        {"id": "kindling", "sustain": 3},
    ],
}

PHASE_NAMES = {"foundation": "the Foundation", "shaft": "the Shaft", "crown": "the Crown", "kindling": "the Kindling"}
PHASE_TEXT = {
    "foundation": "four Ley Pylons and four Warding Stones round the heart, as a Lesser Circle has them",
    "shaft": "three blocks of dark stone stacked on the heart, and the configuration's practice carried through twice",
    "crown": "the configuration's crown on the shaft, and the Kindling rite completed at a Lesser Circle within 12 blocks",
    "kindling": "the spire's upkeep held three days in a row",
}

_CIRCLE = {"research": rid("circle_lore"), "state": "understood"}

# Each configuration: its name, tradition, the research its founder needs, the practice its keepers carry through, its
# crown (the blocks over the shaft), its daily upkeep, what its field does, and the codex's words for it.
CONFIGURATIONS = {
    "lantern_spire": {
        "name": "Lantern Spire", "tradition": "lampwrights",
        "requires": [{"research": rid("first_light"), "state": "mastered"}, _CIRCLE],
        "practice": rid("ritual"), "crown": rid("spire_crown_lantern"),
        "crown_blocks": [("minecraft:glowstone", [0, 4, 0]), (rid("lumen_sconce"), [0, 5, 0])],
        "upkeep": {"item": "minecraft:glowstone_dust", "count": 4, "ley": 8},
        "field": {"kind": "illumination", "radius": 24, "count": 4},
        "needs": "First Light mastered and Circle Lore understood; its keepers complete rituals",
        "field_text": "kindles light in the dark open air within %s blocks, so nothing hostile spawns there",
    },
    "verdant_spire": {
        "name": "Verdant Spire", "tradition": "greenwardens",
        "requires": [{"research": rid("verdant_husbandry"), "state": "mastered"}, _CIRCLE],
        "practice": rid("cultivation"), "crown": rid("spire_crown_verdant"),
        "crown_blocks": [("minecraft:moss_block", [0, 4, 0]), ("minecraft:flowering_azalea", [0, 5, 0])],
        "upkeep": {"item": "minecraft:bone_meal", "count": 8, "ley": 6},
        "field": {"kind": "growth", "radius": 16, "count": 6},
        "needs": "Verdant Husbandry mastered and Circle Lore understood; its keepers tend living devices",
        "field_text": "grows the crops within %s blocks a step at a time",
    },
    "star_spire": {
        "name": "Star Spire", "tradition": "starwatchers",
        "requires": [{"research": rid("celestial_attunement"), "state": "mastered"}, _CIRCLE],
        "practice": rid("observation"), "crown": rid("spire_crown_star"),
        "crown_blocks": [("minecraft:amethyst_block", [0, 4, 0]), ("minecraft:lightning_rod", [0, 5, 0])],
        "upkeep": {"item": "minecraft:glow_ink_sac", "count": 2, "ley": 8},
        "field": {"kind": "focus", "radius": 24, "count": 6},
        "needs": "Celestial Attunement mastered and Circle Lore understood; its keepers observe the sky",
        "field_text": "restores Focus to its keepers within %s blocks and reveals hostile creatures there",
    },
}

# The spire's structures, built round the heart (tools/concordance_rituals.py writes them with its own).
STRUCTURES = {
    "spire_foundation": {
        "name": "Spire Foundation", "anchor": rid("spire_heart"),
        "parts": [
            {"role": "channel", "block": rid("ley_pylon"), "at": [[2, 0, 0], [-2, 0, 0], [0, 0, 2], [0, 0, -2]]},
            {"role": "boundary", "block": "#jugcraft:concordance/ritual_boundary", "at": [[2, 0, 2], [2, 0, -2], [-2, 0, 2], [-2, 0, -2]]},
        ],
    },
    "spire_shaft": {
        "name": "Spire Shaft", "anchor": rid("spire_heart"),
        "parts": [{"role": "boundary", "block": f"#{SPIRE_STONE_TAG}", "at": [[0, 1, 0], [0, 2, 0], [0, 3, 0]]}],
    },
}
for _key, _info in CONFIGURATIONS.items():
    STRUCTURES[_info["crown"].split(":")[1]] = {
        "name": f"{_info['name']} Crown", "anchor": rid("spire_heart"),
        "parts": [{"role": "boundary", "block": block, "at": [at]} for block, at in _info["crown_blocks"]],
    }

# The rite: a ritual at a Lesser Circle near the heart (tools/concordance_rituals.py writes it with its own).
RITUALS = {
    "spire_kindling": {
        "name": "The Kindling",
        "summary": "Kindles a Concord Spire within 12 blocks whose crown stands: each participant is shielded for two "
                   "minutes, and the spire's Crown is finished.",
        "structure": rid("lesser_circle"), "research": rid("circle_lore"), "stage": "understood",
        "participants": 1, "focus": 8, "steps": 6, "ley": 2,
        "offerings": [{"item": "minecraft:glowstone_dust", "count": 8}, {"item": "minecraft:gold_ingot", "count": 2},
                      {"item": "minecraft:ender_pearl", "count": 1}],
        "result": {"effects": [
            {"target": "participants",
             "operation": {"effect": "protection", "intent": "helpful", "principle": "radiance", "magnitude": 8,
                           "duration": 2400}},
        ]},
        "backlash": 4,
    },
}

ITEMS = {}
BLOCKS = {"spire_heart": {"name": "Spire Heart"}}
RESEARCH = {}

RECIPES = {
    "spire_heart": {"pattern": ["GAG", "SCS", "SSS"], "key": {"G": "minecraft:gold_ingot", "A": "#jugcraft:concordance/luminous_matter",
                                                               "C": rid("circle_anchor"), "S": "minecraft:polished_deepslate"}},
}

REASONS = {
    "disabled": "the Concordance is switched off",
    "unknown": "this wonder or configuration is not in the rules",
    "founded": "a spire is already founded here",
    "stage": "you must be a Master of the Concordance first",
    "research": "you do not yet know what this configuration needs",
    "not_yours": "this spire is kept by someone else",
    "no_spire": "you keep no spire within 16 blocks",
}

MISSING = {
    "nothing": "nothing: it finishes at the next look",
    "structure": "the structure standing whole: %s",
    "practices": "%s more: %s",
    "rite": "the Kindling rite completed at a Lesser Circle within %s blocks",
    "sustain": "its upkeep held day after day (%s of %s)",
    "raised": "nothing: it is raised",
}

DORMANT = {
    "raising": "it is still being raised",
    "damaged": "part of it is missing or wrong",
    "unattended": "its keepers have not carried its practice through this week",
    "unsupplied": "its last day's upkeep was not there",
    "disabled": "the Concordance is switched off",
    "unknown": "its wonder or configuration is not in the rules",
}

MESSAGES = {
    "spire.refused": "The spire refuses: %s",
    "spire.founded": "You found a %s. Build its Foundation round the heart.",
    "spire.selected": "%s chosen (%s). Use the heart to found it; sneak and use it to choose another.",
    "spire.returned": "The heart is back: the spire answers again, as it was.",
    "spire.kept_by_another": "A spire is recorded here, kept by someone else.",
    "spire.shared": "The spire is shared with your party.",
    "spire.kept": "The spire is kept for yourself.",
    "spire.stored": "%s %s went into the heart's store",
    "spire.rite": "The Kindling is counted for the spire at %s, %s, %s",
    "spire.advanced": "The spire's %s is finished; now %s.",
    "spire.raised": "The Concord Spire is raised!",
    "spire.title": "%s, kept by %s",
    "spire.title_shared": "%s, kept by %s and their party",
    "spire.away": "someone away",
    "spire.phase": "Phase %s of %s, %s. Needs %s",
    "spire.upkeep": "Upkeep a day: %s %s and %s Ley Charge. In its store: %s; in its pylons: %s Ley. Next day in %s min.",
    "spire.field_working": "Field: working (%s).",
    "spire.field_resting": "Field: resting (%s): %s.",
    "spire.attended": "Attended: %s more days before it needs %s again.",
    "spire.unattended": "Unattended: carry %2$s through to wake it.",
    "spire.realigned": "The spire is realigned as a %s: its crown, rite and days are done again.",
    "spire.abandoned": "You abandon the spire. What you built stays where it is.",
    "spire.none": "You keep no Concord Spire.",
}

TOOLTIPS = {
    "jade.spire_unfounded": "Not founded: sneak and use it to choose a configuration, then use it to found one",
    "jade.spire_raised": "Raised",
    "jade.spire_phase": "Phase %s of %s: %s",
    "jade.spire_working": "Field: working",
    "jade.spire_resting": "Field: resting (%s)",
    "jade.spire_store": "Upkeep in store: %s (a day takes %s)",
}
DEVICE_TOOLTIPS = {
    "spire_heart": "Found a Concord Spire here (a Master's work); use it with an empty hand to see how it stands.",
}

ADVANCEMENTS = {
    "concord_spire_founded": ("A Spire Founded", "Found a Concord Spire at a Spire Heart", "task", rid("spire_heart"),
                              "concordance_stage_master"),
    "concord_spire_raised": ("The Spire Raised", "Raise a Concord Spire and keep it kindled", "challenge", "minecraft:beacon",
                             "concord_spire_founded"),
}


def wonder_json():
    return dict({"schema": 1}, **WONDER)


def configuration_json(key):
    info = CONFIGURATIONS[key]
    return {"schema": 1, "wonder": WONDER_ID, "tradition": info["tradition"], "requires": info["requires"],
            "practice": info["practice"], "crown": info["crown"], "upkeep": info["upkeep"], "field": info["field"]}


def lang_entries(lang):
    for key, text in REASONS.items():
        lang[f"compose.{MOD}.spire.reason.{key}"] = text
    for key, text in MISSING.items():
        lang[f"compose.{MOD}.spire.missing.{key}"] = text
    for key, text in DORMANT.items():
        lang[f"compose.{MOD}.spire.dormant.{key}"] = text
    for key, text in PHASE_NAMES.items():
        lang[f"compose.{MOD}.spire.phase.{key}"] = text
    for key, info in CONFIGURATIONS.items():
        lang[f"compose.{MOD}.spire.configuration.{key}"] = info["name"]
        lang[f"compose.{MOD}.spire.needs.{key}"] = info["needs"]
    for kind in FIELD_KINDS:
        text = next(info["field_text"] for info in CONFIGURATIONS.values() if info["field"]["kind"] == kind)
        lang[f"compose.{MOD}.spire.field.{kind}"] = text
    for key, (title, description, _frame, _icon, _parent) in ADVANCEMENTS.items():
        lang[f"advancements.{MOD}.{key}.title"] = title
        lang[f"advancements.{MOD}.{key}.description"] = description
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"tag.block.{SPIRE_STONE_TAG.replace(':', '.').replace('/', '.')}"] = "Spire Stone"
    lang[f"config.jade.plugin_{MOD}.spire_heart"] = "Spire Heart"


def _days(ticks):
    return ticks // DAY


def codex():
    import concordance_conclave as conclave
    import concordance_rituals as rituals
    configuration_pages = []
    for key, info in CONFIGURATIONS.items():
        upkeep = info["upkeep"]
        crown = " over ".join(block.split(":")[1].replace("_", " ") for block, _at in reversed(info["crown_blocks"]))
        configuration_pages.append(("text", info["name"],
                                    f"**Needs:** {info['needs']}.\\\n**Crown:** {crown}.\\\n**Upkeep a day:** "
                                    f"{conclave._amount(upkeep['count'], upkeep['item'])} and {upkeep['ley']} "
                                    f"Ley Charge.\\\n**Its field** {info['field_text'] % info['field']['radius']}, at most "
                                    f"{info['field']['count']} at each pulse (every {WONDER['pulse'] // 20} seconds)."))
    phase_lines = "\\\n".join(f"{i + 1}. **{PHASE_NAMES[phase['id']].removeprefix('the ').title()}**: {PHASE_TEXT[phase['id']]}."
                              for i, phase in enumerate(WONDER["phases"]))
    return {
        ("wonders", "concord_spire"): {
            "name": "The Concord Spire", "x": 0, "y": 0, "icon": rid("spire_heart"), "condition": None,
            "description": "The Concordance's endgame wonder: founded, raised and kept",
            "pages": [
                ("text", "The Concord Spire",
                 "A Concord Spire is raised round a **Spire Heart** by a **Master** of the Concordance, and works only "
                 "while it is kept: supplied every day, whole, and attended by keepers who go on practising its "
                 "tradition. It is founded in one of three **configurations**, each with its own research, practice, "
                 "crown, upkeep and field. A spire is the world's, not the heart's: breaking the heart, leaving or a "
                 "restart loses nothing, and a heart put back where it stood answers again."),
                ("crafting_recipe", "The Spire Heart",
                 "Place the heart where the spire will stand. **Sneak and use** it with an empty hand to choose a "
                 "configuration (it says what each needs), then **use** it to found the spire. Afterwards an empty "
                 "hand shows how it stands; its keeper sneaking shares it with their party or takes it back.",
                 rid("spire_heart")),
                ("text", "Raising It", "A spire is raised in four phases, in order:\\\n" + phase_lines),
                ("text", "The Kindling",
                 "The Crown is finished by **the Kindling**, a ritual at a Lesser Circle within "
                 f"{WONDER['rite_range']} blocks of the heart, taught by Circle Lore: " + rituals._ritual_text("spire_kindling")
                 if "spire_kindling" in rituals.RITUALS else "The Crown is finished by the Kindling."),
            ],
        },
        ("wonders", "configurations"): {
            "name": "Configurations", "x": 2, "y": 0, "icon": "minecraft:glowstone", "condition": None,
            "description": "Lantern, Verdant and Star",
            "pages": configuration_pages + [
                ("text", "Realigning",
                 "**/jugcraft concordance spire realign <configuration>** changes the configuration of the spire you "
                 "keep nearest you, if you know what the new one needs. Before the Crown nothing is lost; after it, "
                 "the Foundation and the Shaft stand, and the new crown, its Kindling and its days are done again."),
            ],
        },
        ("wonders", "keeping"): {
            "name": "Keeping a Spire", "x": 0, "y": 2, "icon": "minecraft:glowstone_dust", "condition": None,
            "description": "Upkeep, attendance, damage and repair",
            "pages": [
                ("text", "Upkeep",
                 "From the Kindling on, each day the spire takes its configuration's item from the heart's store and "
                 "Ley Charge from its own pylons. Put the item in by hand (use it on the heart), by hopper, or let a "
                 f"**courier** bring it: the heart files a request at a Courier Post within {REACH} blocks you may use "
                 "whenever it holds less than two days' upkeep. A day without its upkeep takes nothing and the field "
                 "rests until the next day that has it."),
                ("text", "Attendance",
                 f"A spire answers keepers who keep practising: its configuration's practice carried through by its "
                 f"keeper (or, if shared, their party) at least once every {WONDER['attendance_days']} days. Unattended, "
                 "it rests; the next practice wakes it."),
                ("text", "Damage and Repair",
                 "The heart checks every part of the spire it has raised. If one is missing or wrong, the spire rests "
                 "and says which and where; put it back and it works again at once. Nothing is lost: not its phase, "
                 "not its days. The spire never breaks or replaces a block itself: its light goes only into open air "
                 "and its growth only into crops. A Verdant Bed's crop it hastens grows by its own rules: its bed pays "
                 "at least a nutrient a step, and a Mendvetch fixes none while hastened."),
                ("text", "Inspecting",
                 "Use the heart with an empty hand, or **/jugcraft concordance spire**, for its phase and what it "
                 "still needs, its upkeep and store, its field and its attendance. Jade, when installed, shows the "
                 "same at a glance. **/jugcraft concordance spire abandon confirm** forgets the spire you keep nearest "
                 "you; what you built stays."),
            ],
        },
    }


CATEGORY = {"wonders": {"name": "Wonders", "icon": rid("spire_heart"), "sort": 15,
                        "description": "Endgame works raised together and kept: the Concord Spire"}}


def tags(tags):
    for block in SPIRE_STONES:
        tags.add("block", SPIRE_STONE_TAG, block)
    tags.add("block", "minecraft:mineable/pickaxe", rid("spire_heart"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_spire_models as models
    write(assets / "geckolib" / "models" / "block" / "spire_heart.geo.json", models.GEO["spire_heart"]())
    write(assets / "geckolib" / "animations" / "block" / "spire_heart.animation.json", models.ANIMATIONS["spire_heart"]())
    write(assets / "blockstates" / "spire_heart.json", {"variants": {"lit=false": {"model": rid("block/spire_heart")},
                                                                     "lit=true": {"model": rid("block/spire_heart")}}})
    # The block model only gives the particle; GeckoLib draws the heart.
    write(assets / "models" / "block" / "spire_heart.json", {"textures": {"particle": rid("block/spire_heart")}})
    write(assets / "items" / "spire_heart.json", {"model": {"type": "minecraft:model", "model": rid("item/spire_heart")}})
    write(assets / "models" / "item" / "spire_heart.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/spire_heart")}})
    write(data / "loot_table" / "blocks" / "spire_heart.json", self_drop("spire_heart"))
    for name, entry in RECIPES.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": entry["pattern"], "key": entry["key"],
            "result": {"id": rid(name), "count": 1}}))
    for key, (_title, _description, frame, icon, parent) in ADVANCEMENTS.items():
        write(data / "advancement" / f"{key}.json", {
            "parent": rid(parent),
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{MOD}.{key}.title"},
                        "description": {"translate": f"advancements.{MOD}.{key}.description"}, "frame": frame,
                        "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})


def write_data(write, data):
    write(data / "concordance" / "wonder" / "concord_spire.json", wonder_json())
    for key in CONFIGURATIONS:
        write(data / "concordance" / "wonder_configuration" / f"{key}.json", configuration_json(key))
