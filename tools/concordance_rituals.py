"""Roadmap step 12: ritual structures and execution states (docs/features/arcane-concordance-rituals.md).

The Circlewrights' practice. A ritual is worked at a Circle Anchor inside a structure built round it: Ley Pylons carry
Ley Charge in (channels), Warding Stones hold the working in (boundary), and the air above the anchor stays open
(clearance). The rules are data (data/jugcraft/concordance/structure and /ritual, read by Java's
concordance/ritual/StructurePattern and RitualDefinition); the lifecycle every ritual shares is Java's RitualMachine and
RitualRun, carried out by CircleAnchorBlockEntity.

tools/concordance.py merges these tables into its own (RESEARCH, ITEMS, BLOCKS, INSTRUMENTS, CONVERSIONS, codex and
language), so the generator, the checker and the codex treat them like First Light's. Numbers the Java repeats are
checked by tools/check_mod_data.py.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# The lifecycle (Java: ritual/RitualMachine.java).
STEP_TICKS = 40  # a ritual checks its structure and draws Ley Charge once a step: every two seconds
GATHER_TICKS = 600  # how long the others have to join once the leader starts: 30 seconds
PARTICIPANT_MARGIN = 4  # a participant stands within the structure's reach plus this, horizontally
PARTICIPANT_HEIGHT = 3  # ...and within this many blocks above or below the anchor
# The anchor (Java: CircleAnchorBlockEntity.java).
ANCHOR_SLOTS = 6  # offering slots (Java also RitualDefinition.MAX_OFFERINGS)
REPORT_CACHE_TICKS = 100  # an idle anchor reuses its structure report this long unless a change nearby voids it
# The Ley Pylon (Java: LeyPylonBlockEntity.java).
PYLON_CAPACITY = 64  # Ley Charge held
PYLON_POUR = 15  # the most Radiance one pour from a Kindled Lantern turns into Ley Charge (whole batches only)
JE_PER_LEY = 1000  # Jugcraft Energy for one Ley Charge, through the shared energy interface
PYLON_JE_RATE = 64  # JE a tick a pylon accepts (eight solar panels' worth)
RADIANCE_PER_LEY_BATCH = 3  # radiance_to_ley: 3 Radiance become 2 Ley Charge
LEY_PER_BATCH = 2
# Mastery: complete rituals in this many different chunks.
RITUAL_MASTERY = 3

CIRCLE_SPECIMEN_TAG = f"{MOD}:circle_specimens"
BOUNDARY_TAG = f"{MOD}:concordance/ritual_boundary"
# What a Circlewright learns from: things made to hold a shape or a bearing.
CIRCLE_SPECIMENS = {
    "minecraft:compass": "Compass",
    "minecraft:lead": "Lead",
    "minecraft:calcite": "Calcite",
    "minecraft:chiseled_stone_bricks": "Chiseled Stone Bricks",
}

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "circle_lore": {
        "name": "Circle Lore",
        "principle": "tether",
        "tradition": "circlewrights",
        "stage": "practitioner",
        "icon": "minecraft:compass",
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{CIRCLE_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{CIRCLE_SPECIMEN_TAG}", "distinct": 2}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{CIRCLE_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: rituals carried through to the end in different places.
            "mastered": [{"type": "practice", "activity": rid("ritual"), "distinct": RITUAL_MASTERY}],
        },
        "unlocks": {"understood": {"rituals": [rid("adept_attunement"), rid("lumen_vigil")]}},
        # Advancement titles and descriptions, and the icon of each state's advancement.
        "advancements": {
            "encountered": ("Holding a Bearing", "Examine something made to hold a shape or a bearing", "task",
                            "minecraft:compass"),
            "observed": ("Lines That Hold", "Examine two different circle specimens", "task", "minecraft:lead"),
            "understood": ("Circle Lore", "Understand circles: study a circle specimen at a Lampwright's Bench", "goal",
                           rid("circle_anchor")),
            "mastered": ("Circlewright", f"Complete rituals in {RITUAL_MASTERY} different chunks", "challenge",
                         rid("adept_wand")),
        },
        "locked": {"encountered": "Examine a circle specimen first",
                   "observed": "Examine two different circle specimens first",
                   "understood": "Understand Circle Lore first", "mastered": "Master Circle Lore first"},
    },
}

# ------------------------------------------------------------------------------------------------- structures

STRUCTURES = {
    # Seven blocks across: a ring of eight Warding Stones round four Ley Pylons, the anchor at the centre and open air
    # above it. Symmetric, so it works built facing any way (patterns are never rotated).
    "lesser_circle": {
        "name": "Lesser Circle",
        "anchor": rid("circle_anchor"),
        "parts": [
            {"role": "channel", "block": rid("ley_pylon"), "at": [[2, 0, 0], [-2, 0, 0], [0, 0, 2], [0, 0, -2]]},
            {"role": "boundary", "block": f"#{BOUNDARY_TAG}",
             "at": [[3, 0, 0], [-3, 0, 0], [0, 0, 3], [0, 0, -3], [2, 0, 2], [2, 0, -2], [-2, 0, 2], [-2, 0, -2]]},
            {"role": "clearance", "at": [[0, 1, 0], [0, 2, 0]]},
        ],
    },
}

STRUCTURE_LIMITS = {"MAX_PARTS": 64, "MAX_REACH": 8, "MAX_CHANNELS": 16}  # Java: StructurePattern
RITUAL_LIMITS = {"MAX_PARTICIPANTS": 4, "MAX_FOCUS": 100, "MAX_STEPS": 30, "MAX_LEY": 16, "MAX_OFFERINGS": ANCHOR_SLOTS,
                 "MAX_COUNT": 64, "MAX_BACKLASH": 20, "MAX_GRANTS": 4, "MAX_RADIUS": 16,
                 "MAX_TARGETS": 16}  # Java: RitualDefinition

# ------------------------------------------------------------------------------------------------- rituals

RITUALS = {
    # The way to a better instrument: a solo working that attunes an Initiate's Wand into an Adept's Wand, keeping
    # whatever was inscribed and tuned on it.
    "adept_attunement": {
        "name": "Adept's Attunement",
        "summary": "Attunes an Initiate's Wand into an Adept's Wand, which holds larger spells. What is inscribed "
                   "and tuned on the wand stays.",
        "structure": rid("lesser_circle"), "research": rid("circle_lore"), "stage": "understood",
        "participants": 1, "focus": 6, "steps": 5, "ley": 2,
        "offerings": [{"item": rid("initiate_wand"), "count": 1}, {"item": "minecraft:amethyst_shard", "count": 4},
                      {"item": "minecraft:gold_ingot", "count": 2}, {"item": "minecraft:glowstone_dust", "count": 4}],
        "result": {"transform": {"from": rid("initiate_wand"), "into": rid("adept_wand")}},
        "backlash": 4,
    },
    # A collaboration milestone, never a gate: two practitioners keep a vigil in the dark and are shielded for two
    # minutes, while every creature nearby is outlined.
    "lumen_vigil": {
        "name": "Lumen Vigil",
        "summary": "Two practitioners keep a vigil in the dark: each is shielded, and creatures nearby are outlined.",
        "structure": rid("lesser_circle"), "research": rid("circle_lore"), "stage": "understood",
        "participants": 2, "focus": 4, "steps": 3, "ley": 1, "conditions": {"max_light": 7},
        "offerings": [{"item": "minecraft:glow_berries", "count": 4}],
        "result": {"effects": [
            {"target": "participants",
             "operation": {"effect": "protection", "intent": "helpful", "principle": "radiance", "magnitude": 8,
                           "duration": 2400}},
            {"target": "creatures", "radius": 16, "targets": 16,
             "operation": {"effect": "detection", "intent": "harmful", "principle": "radiance", "duration": 1200}},
        ]},
        "backlash": 2,
    },
}

CONVERSIONS = {
    # A Kindled Lantern poured into a Ley Pylon: Radiance becomes Ley Charge, always losing a third.
    "radiance_to_ley": {"from": {"resource": "essence/radiance", "amount": RADIANCE_PER_LEY_BATCH},
                        "to": {"resource": "ley_charge", "amount": LEY_PER_BATCH}},
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "adept_wand": {"name": "Adept's Wand",
                   "tooltip": "A second instrument, attuned in a circle: it holds larger spells than an Initiate's "
                              "Wand. Hold it in the main hand to cast."},
}
BLOCKS = {
    "circle_anchor": {"name": "Circle Anchor"},
    "ley_pylon": {"name": "Ley Pylon"},
    "warding_stone": {"name": "Warding Stone"},
}
INSTRUMENTS = ["adept_wand"]
INSTRUMENT_LIMITS = {
    "adept_wand": {"item": rid("adept_wand"), "capacity": 12, "targets": 8, "work": 72, "branches": 2, "duration": 2400},
}

# ------------------------------------------------------------------------------------------------- text

MESSAGES = {
    "circle.status": "%s: %s",
    "circle.not_anchor": "That is not a Circle Anchor",
    "circle.complete": "The circle is complete",
    "circle.faults": "%s faults: %s",
    "circle.fault.missing": "%s missing at %s",
    "circle.fault.incompatible": "%s at %s is the wrong block",
    "circle.fault.obstructed": "Clear the space at %s",
    "circle.fault.unpowered": "The pylon at %s needs Ley Charge",
    "circle.fault.foreign": "The pylon at %s belongs to someone else",
    "circle.fault.unloaded": "%s is out of reach",
    "circle.role.channel": "Ley Pylon",
    "circle.role.boundary": "Warding Stone",
    "circle.role.clearance": "Open space",
    "circle.phase.idle": "Waiting",
    "circle.phase.gathering": "Gathering: %s of %s",
    "circle.phase.channeling": "Channeling: step %s of %s",
    "circle.phase.complete": "Complete: take the result",
    "circle.offered": "Offered %s",
    "circle.returned": "Returned the offerings",
    "circle.locked": "The offerings are held until the ritual ends",
    "circle.full": "The anchor's slots are full",
    "circle.nothing": "Nothing is offered",
    "circle.no_match": "These offerings answer no ritual you know",
    "circle.missing": "%s needs %s more %s",
    "circle.unknown": "You have not learned %s (the codex says how)",
    "circle.no_focus": "%s needs %s Focus",
    "circle.started": "%s begins",
    "circle.waiting": "%s: %s more must join (use the anchor with an empty hand)",
    "circle.joined": "You join %s",
    "circle.already": "You are already part of this ritual",
    "circle.leader_only": "Only the one who began it can call it off",
    "circle.busy": "A ritual is already under way here",
    "circle.take_first": "Take the last result first",
    "circle.too_far": "Stand closer to the anchor",
    "circle.too_bright": "%s needs light %s or less above the anchor",
    "circle.completed": "%s is complete",
    "circle.interrupted.structure": "%s broke off: the circle is broken",
    "circle.interrupted.containment": "%s broke off: the boundary failed and the working lashes out",
    "circle.interrupted.power": "%s broke off: a pylon ran dry",
    "circle.interrupted.participants": "%s broke off: not everyone is here",
    "circle.interrupted.conditions": "%s broke off: it is too bright",
    "circle.interrupted.unloaded": "%s broke off: part of the circle is out of reach",
    "circle.interrupted.lapsed": "%s lapsed while no one was near",
    "circle.interrupted.cancelled": "%s was called off",
    "circle.interrupted.removed": "%s broke off: the anchor is gone",
    "circle.interrupted.disabled": "%s broke off: the Concordance is switched off",
    "circle.interrupted.forgotten": "%s broke off: it is no longer known",
    "circle.interrupted.tampered": "%s broke off: the offerings were disturbed",
    "circle.kept": "The offerings are still in the anchor",
    "pylon.status": "Ley Pylon: %s / %s Ley Charge",
    "pylon.poured": "Poured %s Radiance: %s Ley Charge (%s / %s)",
    "pylon.full": "The pylon is full",
    "pylon.too_little": "Pour at least %s Radiance at a time",
    "pylon.lantern_empty": "The lantern has no Radiance to pour",
    "pylon.settling": "Let the charge settle a moment",
    "examine.form": "You turn the %s over in your hands and note how it holds its shape",
}

TOOLTIPS = {
    "circle_specimen": "Circle specimen: sneak and use to examine it",
    "jade.phase": "%s",
    "jade.offering": "Offering: %s x%s",
    "jade.fault": "%s",
    "jade.ley": "Ley Charge: %s / %s",
    "jade.participants": "Participants: %s",
}

SOUND_EVENTS = {
    "concordance.circle_start": "Circle awakens",
    "concordance.circle_step": "Circle hums",
    "concordance.circle_complete": "Ritual completes",
    "concordance.circle_break": "Ritual breaks",
}


def lang_entries(lang):
    lang[f"tag.item.{CIRCLE_SPECIMEN_TAG.replace(':', '.')}"] = "Circle Specimens"
    lang[f"tag.block.{BOUNDARY_TAG.replace(':', '.').replace('/', '.')}"] = "Ritual Boundary"
    for key, info in RITUALS.items():
        lang[f"ritual.{MOD}.{key}"] = info["name"]
    for key, info in STRUCTURES.items():
        lang[f"structure.{MOD}.{key}"] = info["name"]
    lang[f"config.jade.plugin_{MOD}.circle_anchor"] = BLOCKS["circle_anchor"]["name"]
    lang[f"config.jade.plugin_{MOD}.ley_pylon"] = BLOCKS["ley_pylon"]["name"]
    lang[f"pack.{MOD}.fusion_textures"] = "Jugcraft: connected textures (Fusion)"
    lang[f"jei.{MOD}.concordance.ritual"] = "%s steps in a Lesser Circle; needs Circle Lore"


# ------------------------------------------------------------------------------------------------- codex

def _seconds(ticks):
    value = ticks / 20
    return str(int(value)) if value == int(value) else f"{value:g}"


def _item_name(ref):
    names = {"minecraft:amethyst_shard": "Amethyst Shard", "minecraft:gold_ingot": "Gold Ingot",
             "minecraft:glowstone_dust": "Glowstone Dust", "minecraft:glow_berries": "Glow Berries",
             "minecraft:ender_pearl": "Ender Pearl",
             rid("initiate_wand"): "Initiate's Wand", rid("adept_wand"): "Adept's Wand"}
    return names[ref]


def _ritual_text(key):
    info = RITUALS[key]
    channels = sum(len(p["at"]) for p in STRUCTURES[info["structure"].split(":")[1]]["parts"] if p["role"] == "channel")
    offerings = ", ".join(f"{o['count']} {_item_name(o['item'])}" for o in info["offerings"])
    people = "one practitioner" if info["participants"] == 1 else f"{info['participants']} practitioners"
    light = (f" It must be dark: light {info['conditions']['max_light']} or less above the anchor."
             if "conditions" in info else "")
    return (f"{info['summary']}\\\n\\\n**Offer:** {offerings}.\\\n**Takes:** {people}, {info['focus']} Focus each, "
            f"{info['steps']} steps ({_seconds(info['steps'] * STEP_TICKS)} seconds), drawing {info['ley']} Ley Charge "
            f"from each pylon a step ({info['ley'] * info['steps'] * channels} in all).{light}")


def codex(initiate):
    """Codex entries for the Circles category, in tools/concordance.py's format (a condition names a state of Circle
    Lore as (research, state)). {initiate} is the Initiate's Wand's limits, to compare the Adept's with."""
    lore = "circle_lore"
    specimens = "\\\n".join(f"- {name}" for name in CIRCLE_SPECIMENS.values())
    return {
        ("circles", "circle_lore"): {
            "name": "Circle Lore", "x": 0, "y": 0, "icon": "minecraft:compass", "condition": None,
            "description": "Circles: research",
            "pages": [
                ("text", "Circle Lore",
                 "Once you understand First Light, look at things made to **hold a shape or a bearing**. **Sneak and "
                 "use** one to examine it.\\\n\\\nCircle specimens:\\\n" + specimens),
                ("text", "Encountered", "Examine a second, different circle specimen.", (lore, "encountered")),
                ("text", "Observed", "Study a circle specimen at a **Lampwright's Bench**, or read **Research Notes** "
                                     "from someone who understands Circle Lore.", (lore, "observed")),
                ("text", "Understood",
                 f"You can now work **rituals**: build a Lesser Circle and begin one at its anchor.\\\n\\\nTo **master** "
                 f"Circle Lore, complete rituals in {RITUAL_MASTERY} different chunks.", (lore, "understood")),
                ("text", "Mastered", "Your circles hold. Rituals you complete still count for the codex, but mastery "
                                     "asks nothing more of you.", (lore, "mastered")),
            ],
        },
        ("circles", "lesser_circle"): {
            "name": "The Lesser Circle", "x": 2, "y": 0, "icon": rid("circle_anchor"), "condition": (lore, "encountered"),
            "description": "Anchor, channels, boundary",
            "pages": [
                ("crafting_recipe", "Circle Anchor",
                 "A ritual is worked at a **Circle Anchor**, inside a circle built round it.", rid("circle_anchor")),
                ("text", "Building It",
                 "With the anchor at the centre:\\\n- a **Ley Pylon** two blocks out to the north, south, east and west "
                 "(the **channels**);\\\n- a **Warding Stone** three blocks out in those four directions and two out "
                 "on each diagonal (the **boundary**);\\\n- two blocks of **open air** above the anchor.\\\n\\\nAll "
                 "on the anchor's level. It works built facing any way."),
                ("text", "Reading It",
                 "Use the anchor with an empty hand to hear what is wrong: a part **missing**, the **wrong block**, "
                 "the space **obstructed**, a pylon **without charge** or **someone else's**, or part of it **out of "
                 "reach** (an unloaded chunk). Jade shows the first faults at a glance, and "
                 "`/jugcraft concordance circle` reads the whole circle."),
                ("crafting_recipe", "Warding Stone", "Any block in the ritual boundary tag will hold; these are made "
                                                     "for it.", rid("warding_stone")),
            ],
        },
        ("circles", "ley_pylon"): {
            "name": "Ley Pylon", "x": 4, "y": 0, "icon": rid("ley_pylon"), "condition": (lore, "encountered"),
            "description": "Ley Charge for rituals",
            "pages": [
                ("crafting_recipe", "Ley Pylon",
                 f"A pylon holds up to {PYLON_CAPACITY} **Ley Charge**, the working energy rituals draw. Each step of "
                 f"a ritual draws from every pylon in the circle at once, all or none.", rid("ley_pylon")),
                ("text", "Charging",
                 f"**From a Kindled Lantern:** use the lantern on the pylon. Every {RADIANCE_PER_LEY_BATCH} Radiance "
                 f"becomes {LEY_PER_BATCH} Ley Charge, up to {PYLON_POUR} Radiance a pour; what does not convert stays "
                 f"in the lantern.\\\n\\\n**From electricity:** connect any Jugcraft power source. Every {JE_PER_LEY} JE "
                 f"becomes 1 Ley Charge, at up to {PYLON_JE_RATE} JE a tick.\\\n\\\nNothing turns Ley Charge back."),
                ("text", "Whose Pylon",
                 "The pylon belongs to whoever placed it. A ritual draws only from pylons owned by someone taking "
                 "part (or by no one)."),
            ],
        },
        ("circles", "rituals"): {
            "name": "Working a Ritual", "x": 0, "y": 2, "icon": rid("initiate_wand"), "condition": (lore, "understood"),
            "description": "What a ritual takes, and what it risks",
            "pages": [
                ("text", "Beginning",
                 f"Put the **offerings** in the anchor (use it with each). Then use it with an **empty hand**: "
                 f"the anchor finds the ritual the offerings answer, checks the circle, and takes your **Focus**. "
                 f"A ritual for more than one waits {_seconds(GATHER_TICKS)} seconds for the others, who join the "
                 f"same way. **Sneak** with an empty hand to take your offerings back before it begins."),
                ("text", "While It Runs",
                 f"Every {_seconds(STEP_TICKS)} seconds the ritual checks the whole circle again and draws Ley Charge. "
                 f"Every participant must stay within {PARTICIPANT_MARGIN} blocks of the circle's edge. The "
                 f"offerings are **held**: nothing can be added or taken until it ends. The leader can call it off "
                 f"by sneaking with an empty hand."),
                ("text", "How It Ends",
                 "**Completed:** the offerings are used and the result is made, both at the same moment, once.\\\n\\\n"
                 "**Broken off** (a part broken, a pylon dry, someone gone, too bright, the anchor's chunk unloaded or "
                 "the server restarted): nothing is made, the offerings stay in the anchor for you to take back or "
                 "try again, and the Focus and Ley Charge already spent are gone."),
                ("text", "Containment",
                 "If a **Warding Stone** is lost while a ritual runs, the working lashes out: every participant "
                 "takes its backlash. The offerings are still returned. If the anchor itself is broken, they drop "
                 "where it stood."),
            ],
        },
        ("circles", "adept_attunement"): {
            "name": "Adept's Attunement", "x": 2, "y": 2, "icon": rid("adept_wand"), "condition": (lore, "understood"),
            "description": "A larger instrument",
            "pages": [
                ("text", "Adept's Attunement", _ritual_text("adept_attunement")),
                ("text", "The Adept's Wand",
                 "An Adept's Wand holds spells of up to {capacity} capacity, {targets} targets and {work} work, with "
                 "up to {branches} branches (the Initiate's Wand: {i_capacity}, {i_targets}, {i_work} and "
                 "{i_branches}). Take it from the anchor once the ritual completes.".format(
                     **INSTRUMENT_LIMITS["adept_wand"], **{f"i_{k}": v for k, v in initiate.items()})),
            ],
        },
        ("circles", "lumen_vigil"): {
            "name": "Lumen Vigil", "x": 4, "y": 2, "icon": "minecraft:glow_berries", "condition": (lore, "understood"),
            "description": "A vigil kept together",
            "pages": [
                ("text", "Lumen Vigil", _ritual_text("lumen_vigil")),
                ("text", "Its Effect",
                 "Each participant gains 8 points of absorption for 2 minutes, and up to 16 creatures within 16 "
                 "blocks glow for a minute. It needs two people, but nothing else in the Concordance needs it."),
            ],
        },
    }


CATEGORY = {"circles": {"name": "Circles", "icon": "minecraft:compass", "sort": 4,
                        "description": "The Circlewrights' practice: rituals, their circles and their risks"}}


# ------------------------------------------------------------------------------------------------- data and assets

def structure_json(info):
    return {"schema": 1, "anchor": info["anchor"], "parts": info["parts"]}


def ritual_json(info):
    keys = ("structure", "research", "stage", "participants", "focus", "steps", "ley", "conditions", "offerings",
            "result", "backlash")
    return dict({"schema": 1}, **{k: info[k] for k in keys if k in info})


def tags(tags):
    for ref in CIRCLE_SPECIMENS:
        tags.add("item", CIRCLE_SPECIMEN_TAG, ref)
    tags.add("block", BOUNDARY_TAG, rid("warding_stone"))
    tags.add("block", "minecraft:mineable/pickaxe", rid("circle_anchor"))
    tags.add("block", "minecraft:mineable/pickaxe", rid("ley_pylon"))
    tags.add("block", "minecraft:mineable/pickaxe", rid("warding_stone"))


def _faces(texture, uv=None):
    return {d: dict({"texture": f"#{texture}"}, **({"uv": uv} if uv else {}))
            for d in ("north", "east", "south", "west", "up", "down")}


def _box(f, t, texture, emissive=False):
    element = {"from": f, "to": t, "faces": _faces(texture)}
    if emissive:
        element["light_emission"] = 15
    return element


def pylon_elements(charged):
    """A Ley Pylon: a stone plinth, four copper posts and a lapis crystal held between them, lit while charged."""
    return [
        _box([2, 0, 2], [14, 3, 14], "stone"),
        _box([3, 3, 3], [13, 4, 13], "copper"),
        _box([3, 4, 3], [5, 15, 5], "copper"), _box([11, 4, 3], [13, 15, 5], "copper"),
        _box([3, 4, 11], [5, 15, 13], "copper"), _box([11, 4, 11], [13, 15, 13], "copper"),
        _box([6, 5, 6], [10, 13, 10], "crystal", emissive=charged),
        _box([3, 15, 3], [13, 16, 13], "copper"),
    ]


def anchor_geo():
    """The Circle Anchor for GeckoLib: a stone plinth with a copper collar, a brass ring on three posts, and a lapis
    crystal floating above, which the animations turn and lift. Box UVs into a 64x64 sheet laid out by
    anchor_uv_layout (tools/concordance_ritual_art.py paints the same regions)."""
    bones = [
        {"name": "base", "pivot": [0, 0, 0], "cubes": [
            {"origin": [-7, 0, -7], "size": [14, 4, 14], "uv": [0, 0]},
            {"origin": [-5, 4, -5], "size": [10, 2, 10], "uv": [0, 18]},
        ]},
        {"name": "ring", "parent": "base", "pivot": [0, 9, 0], "cubes": [
            {"origin": [-6, 8, -1], "size": [1, 2, 2], "uv": [40, 18]},
            {"origin": [5, 8, -1], "size": [1, 2, 2], "uv": [40, 18]},
            {"origin": [-1, 8, -6], "size": [2, 2, 1], "uv": [40, 23]},
            {"origin": [-1, 8, 5], "size": [2, 2, 1], "uv": [40, 23]},
            {"origin": [-5, 8.5, -5], "size": [10, 1, 1], "uv": [0, 30]},
            {"origin": [-5, 8.5, 4], "size": [10, 1, 1], "uv": [0, 30]},
            {"origin": [-5, 8.5, -4], "size": [1, 1, 8], "uv": [0, 32]},
            {"origin": [4, 8.5, -4], "size": [1, 1, 8], "uv": [0, 32]},
        ]},
        {"name": "posts", "parent": "base", "pivot": [0, 6, 0], "cubes": [
            {"origin": [-4.5, 6, -0.5], "size": [1, 3, 1], "uv": [48, 18]},
            {"origin": [3.5, 6, -0.5], "size": [1, 3, 1], "uv": [48, 18]},
            {"origin": [-0.5, 6, 3.5], "size": [1, 3, 1], "uv": [48, 18]},
        ]},
        {"name": "crystal", "parent": "base", "pivot": [0, 13, 0], "cubes": [
            {"origin": [-2, 10, -2], "size": [4, 6, 4], "uv": [24, 32]},
            {"origin": [-1, 16, -1], "size": [2, 2, 2], "uv": [40, 32]},
            {"origin": [-1, 9, -1], "size": [2, 1, 2], "uv": [40, 32]},
        ]},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.circle_anchor", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2.5, "visible_bounds_offset": [0, 1, 0]},
        "bones": bones}]}


def anchor_animations():
    """Idle: the crystal turns slowly and bobs. Channel: it rises, turns fast and the ring counter-turns."""
    return {"format_version": "1.8.0", "animations": {
        "animation.circle_anchor.idle": {"loop": True, "animation_length": 8.0, "bones": {
            "crystal": {"rotation": {"0.0": [0, 0, 0], "8.0": [0, 360, 0]},
                        "position": {"0.0": [0, 0, 0], "4.0": [0, 0.75, 0], "8.0": [0, 0, 0]}}}},
        "animation.circle_anchor.channel": {"loop": True, "animation_length": 2.0, "bones": {
            "crystal": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, 720, 0]},
                        "position": {"0.0": [0, 2.5, 0], "1.0": [0, 3.25, 0], "2.0": [0, 2.5, 0]}},
            "ring": {"rotation": {"0.0": [0, 0, 0], "2.0": [0, -180, 0]}}}},
    }}


def player_animations():
    """Participants' gesture while a ritual channels (PAL, played by the client for players the anchor names): both
    arms held out low towards the anchor, palms down, rising and falling slowly with the steps."""
    moves = []
    for tick, pitch in ((0, -0.85), (20, -1.05), (40, -0.85)):
        for arm, yaw, roll in (("rightArm", -0.25, 0.10), ("leftArm", 0.25, -0.10)):
            moves += [{"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, arm: {"pitch": pitch}},
                      {"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, arm: {"yaw": yaw}},
                      {"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, arm: {"roll": roll}}]
        moves.append({"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, "head": {"pitch": 0.35}})
    return {"circle_channel": {"name": "circle_channel", "author": "Jugcraft", "description": "Arcane Concordance: circle_channel",
                               "emote": {"isLoop": "true", "returnTick": 0, "beginTick": 0, "endTick": 40,
                                         "stopTick": 44, "degrees": False, "moves": moves}}}


def fusion_model():
    """The Warding Stone in the Fusion built-in pack (loaded only with Fusion): the same cube, its faces joined to
    neighbouring Warding Stones from a 4x4 tile sheet."""
    return {"loader": "fusion:model", "type": "connecting", "parent": "minecraft:block/cube_all",
            "textures": {"all": rid("block/warding_stone_connected")}, "connections": {"type": "is_same_block"}}


def write_all(write, assets, data, lang, condition, self_drop, packs):
    lang_entries(lang)
    # The Adept's Wand.
    write(assets / "models" / "item" / "adept_wand.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": rid("item/adept_wand")}})
    write(assets / "items" / "adept_wand.json", {"model": {"type": "minecraft:model", "model": rid("item/adept_wand")}})
    # The Ley Pylon: charged lights its crystal.
    textures = {"particle": rid("block/ley_pylon_stone"), "stone": rid("block/ley_pylon_stone"),
                "copper": rid("block/ley_pylon_copper"), "crystal": rid("block/ley_pylon_crystal")}
    for charged in (False, True):
        name = "ley_pylon_charged" if charged else "ley_pylon"
        write(assets / "models" / "block" / f"{name}.json",
              {"parent": "minecraft:block/block", "textures": textures, "elements": pylon_elements(charged)})
    write(assets / "blockstates" / "ley_pylon.json",
          {"variants": {"charged=false": {"model": rid("block/ley_pylon")},
                        "charged=true": {"model": rid("block/ley_pylon_charged")}}})
    write(assets / "items" / "ley_pylon.json", {"model": {"type": "minecraft:model", "model": rid("block/ley_pylon")}})
    # Broken, a pylon keeps its Ley Charge on the item (LeyPylonBlockEntity.collectImplicitComponents).
    pylon_loot = self_drop("ley_pylon")
    pylon_loot["pools"][0]["entries"][0]["modifier"] = [
        {"type": "minecraft:copy_components", "source": "block_entity", "include": [rid("ley_charge")]}]
    write(data / "loot_table" / "blocks" / "ley_pylon.json", pylon_loot)
    # The Warding Stone: a plain cube; with Fusion, the built-in pack joins neighbouring stones.
    write(assets / "models" / "block" / "warding_stone.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": rid("block/warding_stone")}})
    write(assets / "blockstates" / "warding_stone.json", {"variants": {"": {"model": rid("block/warding_stone")}}})
    write(assets / "items" / "warding_stone.json", {"model": {"type": "minecraft:model", "model": rid("block/warding_stone")}})
    write(data / "loot_table" / "blocks" / "warding_stone.json", self_drop("warding_stone"))
    # Fusion reads its own section of the connected sheet's metadata; vanilla ignores it. The sheet is drawn by
    # tools/concordance_ritual_art.py.
    import concordance_ritual_art
    write(assets / "textures" / "block" / "warding_stone_connected.png.mcmeta", concordance_ritual_art.FUSION_METADATA)
    pack = packs / "fusion_textures"
    write(pack / "pack.mcmeta", {"pack": {"description": "Connected textures for Jugcraft's Warding Stones and Verdant Beds (needs Fusion)",
                                          "min_format": 71, "max_format": 2048}})
    write(pack / "assets" / MOD / "models" / "block" / "warding_stone.json", fusion_model())
    # The Circle Anchor: GeckoLib draws it in the world (its block renders nothing); the item and the breaking
    # particles use a plain model.
    write(assets / "geckolib" / "models" / "block" / "circle_anchor.geo.json", anchor_geo())
    write(assets / "geckolib" / "animations" / "block" / "circle_anchor.animation.json", anchor_animations())
    write(assets / "models" / "block" / "circle_anchor.json", {"textures": {"particle": rid("block/ley_pylon_stone")}})
    write(assets / "blockstates" / "circle_anchor.json", {"variants": {"": {"model": rid("block/circle_anchor")}}})
    write(assets / "models" / "item" / "circle_anchor.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/circle_anchor")}})
    write(assets / "items" / "circle_anchor.json", {"model": {"type": "minecraft:model", "model": rid("item/circle_anchor")}})
    write(data / "loot_table" / "blocks" / "circle_anchor.json", self_drop("circle_anchor"))
    # Recipes.
    feature = "concordance"
    write(data / "recipe" / "circle_anchor.json", {
        "fabric:load_conditions": condition(feature), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" A ", "GCG", "SSS"],
        "key": {"A": "minecraft:amethyst_shard", "G": "minecraft:gold_ingot", "C": "minecraft:compass",
                "S": "minecraft:chiseled_stone_bricks"},
        "result": {"id": rid("circle_anchor"), "count": 1}})
    write(data / "recipe" / "ley_pylon.json", {
        "fabric:load_conditions": condition(feature), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" A ", "CLC", "SSS"],
        "key": {"A": "minecraft:amethyst_shard", "C": "minecraft:copper_ingot", "L": "minecraft:lapis_lazuli",
                "S": "minecraft:stone_bricks"},
        "result": {"id": rid("ley_pylon"), "count": 1}})
    write(data / "recipe" / "warding_stone.json", {
        "fabric:load_conditions": condition(feature), "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["SAS", "ACA", "SAS"],
        "key": {"S": "minecraft:stone_bricks", "A": "minecraft:amethyst_shard", "C": "minecraft:calcite"},
        "result": {"id": rid("warding_stone"), "count": 4}})


def write_data(write, data, assets):
    for key, info in STRUCTURES.items():
        write(data / "concordance" / "structure" / f"{key}.json", structure_json(info))
    for key, info in RITUALS.items():
        write(data / "concordance" / "ritual" / f"{key}.json", ritual_json(info))
    for name, clip in player_animations().items():
        write(assets / "player_animations" / f"{name}.json", clip)


def recipe_station():
    """Rituals that make an item, for JEI (client/compat/JugcraftJeiPlugin "concordance_stations"): what to offer and
    what it makes, a public and reproducible recipe. Rituals whose result is an effect make nothing to show, and
    research is never shown; both are in the codex."""
    rows = []
    for key, info in RITUALS.items():
        transform = info["result"].get("transform")
        if transform is None or any(o["item"].startswith("#") for o in info["offerings"]):
            continue
        rows.append({"kind": "ritual", "in": [[o["item"], o["count"]] for o in info["offerings"]],
                     "out": [transform["into"], 1], "value": info["steps"]})
    return {"block": rid("circle_anchor"), "type": rid("circle_anchor"), "recipes": rows}
