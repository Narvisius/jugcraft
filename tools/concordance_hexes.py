"""Roadmap step 22: sympathetic links, curses, wards and dream travel (docs/features/arcane-concordance-hexes.md).

Sympathy (the Hexweavers). A taglock touched to a creature holds a link to it: strength 100, fading one point every
DECAY_TICKS, never to oneself, never to a player the multiplayer rules forbid harming, never through a linking ward.
Held with a curse's reagent, it casts that curse through the link once every rule allows (Java: hex/Hexes.java):
the link alive and strong enough, the target found in the caster's dimension within MAX_RANGE blocks, the rules
letting the caster harm it, no curse ward, Focus enough. A curse pulses a bounded status through the shared effect
boundary, revalidated at every pulse, and is lifted by its remedy, by its time, or when the rules stop allowing it.
A scrying glass investigates: first a curse's name and remedy, then its caster (unless a scrying ward hides them).
Ward sigils ward their user against one category of operation (linking, cursing, scrying, moving) for twenty
minutes; anyone may use them, and the scrying glass, without research.

Dreams (the Dreamwalkers). At an Oneiric Censer at night, everything a dreamer carries goes into one escrow on them;
they dream in adventure mode within RADIUS blocks of their body and catch dream wisps (three at once, one more every
WISP_TICKS); however the dream ends, the escrow comes back slot by slot, with their place and game mode, never more
experience than they entered with (spent experience stays spent), and the dreamglass the rules allow (Java:
dream/DreamRules.java, dreaming/Dreaming.java). Dreamglass makes ward sigils.

The definitions are data (data/jugcraft/concordance/curse, read by Java's concordance/hex). tools/concordance.py merges
these tables into its own; tools/check_mod_data.py checks the numbers the Java repeats.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Java: hex/Hexes.java, hex/Link.java, hex/Ward.java, hex/HexParser.java.
MAX_RANGE = 128
MAX_CURSES = 3
INVESTIGATE_FOCUS = 2
LINK_FRESH = 100
DECAY_TICKS = 240
WARD_TICKS = 24_000
MAX_AMPLIFIER = 1
MAX_PULSE_DURATION = 600
MIN_PULSE_TICKS = 100
MAX_TOTAL_TICKS = 12_000
MAX_FOCUS = 20
# Java: dream/DreamRules.java.
DREAM_TICKS = 3_600
DREAM_RADIUS = 24
DREAM_WISPS = 3
WISP_TICKS = 600
MAX_CAUGHT = 8
DREAM_FOCUS = 10

# The statuses a curse may give (the checker holds every curse to these): hindrances, never harm.
CURSE_EFFECTS = {"minecraft:slowness", "minecraft:weakness", "minecraft:unluck", "minecraft:mining_fatigue", "minecraft:hunger"}

CURSES = {
    "lethargy": {"name": "Curse of Lethargy", "reagent": "minecraft:cobweb", "status": "minecraft:slowness", "amplifier": 0,
                 "pulse_duration": 200, "pulse_ticks": 400, "total_ticks": 6_000, "strength": 30, "focus": 6,
                 "remedy": "minecraft:sugar"},
    "misfortune": {"name": "Curse of Misfortune", "reagent": "minecraft:rabbit_foot", "status": "minecraft:unluck", "amplifier": 0,
                   "pulse_duration": 600, "pulse_ticks": 600, "total_ticks": 12_000, "strength": 20, "focus": 4,
                   "remedy": "minecraft:amethyst_shard"},
    "frailty": {"name": "Curse of Frailty", "reagent": "minecraft:fermented_spider_eye", "status": "minecraft:weakness", "amplifier": 0,
                "pulse_duration": 200, "pulse_ticks": 400, "total_ticks": 6_000, "strength": 40, "focus": 8,
                "remedy": "minecraft:blaze_powder"},
}

WARDS = {
    "linking": ("Linking", "no taglock can take a link to you", "minecraft:string"),
    "cursing": ("Cursing", "no curse pulse reaches you", "minecraft:amethyst_shard"),
    "scrying": ("Scrying", "no scrying glass can name you as a caster, nor show where you are", "minecraft:ink_sac"),
    "moving": ("Moving", "no one else's Concordance effect can push you", "minecraft:iron_ingot"),
}

HEX_REASONS = {
    "no_link": "the taglock holds no link (use it on a creature first)",
    "expired": "the link has faded",
    "elsewhere": "the target is not in this dimension",
    "not_found": "the target cannot be found",
    "too_far": f"the target is more than {MAX_RANGE} blocks away",
    "not_allowed": "the multiplayer rules do not let you act against them",
    "warded": "a ward stops it",
    "weak": "the link is too weak for that curse",
    "already": "that curse already lies on them",
    "too_many": f"no more than {MAX_CURSES} curses can lie on one creature",
    "no_focus": "not enough Focus",
    "self": "a link to yourself would be no link at all",
    "not_yours": "only whoever took this link can use it",
    "disabled": "the Arcane Concordance is disabled on this server",
    "unknown": "you have not understood Sympathy (the codex says how)",
    "no_reagent": "hold a curse's reagent in your other hand",
    "hidden": "a ward hides them",
    "ended": "the curse has run its course",
    "resting": "the curse rests between pulses",
}

DREAM_REASONS = {
    "disabled": "the Arcane Concordance is disabled on this server",
    "unknown": "you have not understood Dreamwalking (the codex says how)",
    "mode": "only players in survival or adventure mode dream",
    "needs_night": "dreams come only at night",
    "busy": "you cannot dream while riding or sleeping",
    "accessories": "take off your accessories first: nothing worn may come into a dream",
    "no_focus": f"dreaming needs {DREAM_FOCUS} Focus",
}

DREAM_ENDS = {
    "woke": "you woke", "timeout": "the dream ran out", "strayed": "you strayed too far from your body",
    "hurt": "you were hurt", "died": "you died", "disconnected": "you left", "recovered": "the dream was found unfinished",
}

SYMPATHY_SPECIMEN_TAG = f"{MOD}:sympathy_specimens"
SYMPATHY_SPECIMENS = {
    "minecraft:fermented_spider_eye": "Fermented Spider Eye", "minecraft:cobweb": "Cobweb",
    "minecraft:poisonous_potato": "Poisonous Potato", "minecraft:rabbit_foot": "Rabbit's Foot", "minecraft:armor_stand": "Armor Stand",
}
DREAM_SPECIMEN_TAG = f"{MOD}:dream_specimens"
DREAM_SPECIMENS = {
    "minecraft:chorus_fruit": "Chorus Fruit", "minecraft:ender_pearl": "Ender Pearl", "minecraft:white_bed": "White Bed",
    "minecraft:spore_blossom": "Spore Blossom",
}
SYMPATHY_PRACTICE = rid("sympathy")  # Java: Sympathy.ACTIVITY
DREAM_PRACTICE = rid("dream")  # Java: Dreaming.ACTIVITY

# ------------------------------------------------------------------------------------------------- research

def _research(key, name, principle, tradition, icon, specimen_tag, practice, distinct, advancements, locked):
    return {
        "name": name, "principle": principle, "tradition": tradition, "stage": "practitioner", "icon": icon,
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{specimen_tag}"}],
            "observed": [{"type": "examine", "specimens": f"#{specimen_tag}", "distinct": 2}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{specimen_tag}"},
                {"type": "notes"},
            ],
            "mastered": [{"type": "practice", "activity": practice, "distinct": distinct}],
        },
        "unlocks": {},
        "advancements": advancements,
        "locked": locked,
    }


RESEARCH = {
    "sympathy": _research(
        "sympathy", "Sympathy", "tether", "hexweavers", rid("taglock"), SYMPATHY_SPECIMEN_TAG, SYMPATHY_PRACTICE, 3,
        {"encountered": ("Like to Like", "Examine something that carries a creature's likeness", "task", "minecraft:cobweb"),
         "observed": ("Correspondences", "Examine two different sympathetic things", "task", "minecraft:rabbit_foot"),
         "understood": ("Sympathy", "Understand Sympathy: study a likeness at a Lampwright's Bench", "goal", rid("taglock")),
         "mastered": ("Hexweaver", "Cast three different curses", "challenge", rid("scrying_glass"))},
        {"encountered": "Examine something that carries a creature's likeness first",
         "observed": "Examine two different sympathetic things first",
         "understood": "Understand Sympathy first", "mastered": "Master Sympathy first"}),
    "dreamwalking": _research(
        "dreamwalking", "Dreamwalking", "echo", "dreamwalkers", rid("oneiric_censer"), DREAM_SPECIMEN_TAG, DREAM_PRACTICE, 3,
        {"encountered": ("Restless Sleep", "Examine something from the edge of dreams", "task", "minecraft:white_bed"),
         "observed": ("Half-Remembered", "Examine two different dreamlike things", "task", "minecraft:chorus_fruit"),
         "understood": ("Dreamwalking", "Understand Dreamwalking: study a dreamlike thing at a Lampwright's Bench", "goal",
                         rid("oneiric_censer")),
         "mastered": ("Dreamwalker", "Come back from three dreams", "challenge", rid("dreamglass"))},
        {"encountered": "Examine something from the edge of dreams first",
         "observed": "Examine two different dreamlike things first",
         "understood": "Understand Dreamwalking first", "mastered": "Master Dreamwalking first"}),
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "taglock": {"name": "Taglock", "tooltip": "Use it on a creature to take a link to it. Then, with a curse's reagent in "
                                              "your other hand, use it to cast that curse through the link."},
    "scrying_glass": {"name": "Scrying Glass", "tooltip": "Use it to see the curses on you; with a remedy in your other hand, "
                                                          "to lift them; with a taglock, to read its link."},
    "ward_sigil": {"name": "Ward Sigil", "tooltip": "Use it to ward yourself for twenty minutes."},
    "dreamglass": {"name": "Dreamglass", "tooltip": "Brought back from a dream. Ward sigils are made with it."},
}
BLOCKS = {"oneiric_censer": {"name": "Oneiric Censer"}}
DEVICE_TOOLTIPS = {
    "oneiric_censer": "Use it at night to dream: you dream with nothing, near your body, and wake with all you had. Use it "
                      "again to wake.",
}

MESSAGES = {
    "hex.refused": "It fails: %s",
    "hex.linked": "The taglock holds a link to %s",
    "hex.cast": "%s is cast",
    "hex.symptom": "Something weighs on you (a scrying glass would show what)",
    "hex.lifted": "%s lifts",
    "hex.remedied": "The remedy lifts %s curse(s)",
    "hex.no_remedy": "That remedies none of the curses on you",
    "hex.clean": "No curse weighs on you",
    "hex.unknown_curse": "An unknown curse (%s s left): look again to learn more",
    "hex.named_curse": "%s: lifted by %s (%s s left)",
    "hex.traced_curse": "%s: lifted by %s, cast by %s (%s s left)",
    "hex.link_none": "The taglock holds no link",
    "hex.link_status": "Link strength %s / %s: %s",
    "hex.link_distance": "the target is %s blocks away",
    "hex.warded": "You are warded against %s for %s minutes",
    "hex.ward_held": "Warded against %s (%s s left)",
    "hex.blank_sigil": "This sigil wards against nothing",
    "dream.refused": "You cannot dream: %s",
    "dream.begun": "You drift into a dream (at most %s s). Catch the wisps by walking into them; use the censer again to wake.",
    "dream.ended": "You wake: %s. %s dreamglass came back with you.",
    "dream.caught": "Dreamglass: %s / %s",
}
TOOLTIPS = {
    "sympathy_specimen": "A likeness: sneak and use to examine it",
    "dream_specimen": "Dreamlike: sneak and use to examine it",
    "hex.taglock_bound": "It holds a link to %s; links fade with time",
    "hex.sigil": "Wards against %s",
}


def lang_entries(lang):
    lang[f"tag.item.{SYMPATHY_SPECIMEN_TAG.replace(':', '.')}"] = "Likenesses"
    lang[f"tag.item.{DREAM_SPECIMEN_TAG.replace(':', '.')}"] = "Dreamlike Things"
    for key, text in HEX_REASONS.items():
        lang[f"compose.{MOD}.hex.reason.{key}"] = text
    for key, info in CURSES.items():
        lang[f"compose.{MOD}.hex.curse.{key}"] = info["name"]
    for key, (name, _does, _ingredient) in WARDS.items():
        lang[f"compose.{MOD}.hex.ward.{key}"] = name.lower()
    lang[f"compose.{MOD}.hex.ward.none"] = "nothing"
    lang[f"compose.{MOD}.hex.kind.player"] = "a player"
    lang[f"compose.{MOD}.hex.kind.creature"] = "a creature"
    for key, text in DREAM_REASONS.items():
        lang[f"compose.{MOD}.dream.reason.{key}"] = text
    for key, text in DREAM_ENDS.items():
        lang[f"compose.{MOD}.dream.end.{key}"] = text
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"entity.{MOD}.dream_wisp"] = "Dream Wisp"


# ------------------------------------------------------------------------------------------------- codex

def seconds(ticks):
    return f"{ticks / 20:g} s"


def codex():
    curse_lines = []
    for key, info in CURSES.items():
        curse_lines.append(f"- **{info['name']}**: {info['reagent'].split(':')[1].replace('_', ' ')} casts it; "
                           f"{info['status'].split(':')[1].replace('_', ' ')} for {seconds(info['pulse_duration'])} every "
                           f"{seconds(info['pulse_ticks'])}, for {seconds(info['total_ticks'])} in all; needs link strength "
                           f"{info['strength']} and {info['focus']} Focus; lifted by "
                           f"{info['remedy'].split(':')[1].replace('_', ' ')}")
    ward_lines = [f"- **{name}**: {does} (paper, dreamglass and {ingredient.split(':')[1].replace('_', ' ')})"
                  for name, does, ingredient in WARDS.values()]
    hexes = [
        ("text", "Sympathy",
         "Once you understand First Light, examine a likeness (**sneak and use**): a fermented spider eye, a cobweb, a "
         "poisonous potato, a rabbit's foot, an armor stand. Study one at a Lampwright's Bench to understand Sympathy."),
        ("crafting_recipe", "Taglocks and Links",
         f"Use a taglock on a creature to take a **link** to it. A fresh link has strength {LINK_FRESH} and loses one every "
         f"{seconds(DECAY_TICKS)}: it fades in twenty minutes. No link can be taken to yourself, to a player the server's "
         f"rules do not let you harm (PvP off, or your party), or to anyone warded against linking. Every use checks the "
         f"link again: its target must be in your dimension, within {MAX_RANGE} blocks, and still yours to act against.",
         rid("taglock")),
        ("text", "Curses",
         "Hold a linked taglock with a curse's reagent in your other hand and use it. A curse is bounded: a hindrance, "
         "never harm, for minutes, never forever. Each pulse is checked again: it skips while you are elsewhere or out of "
         "range or they are warded, and lifts for good if the rules stop letting you harm them.\\\n" + "\\\n".join(curse_lines)),
        ("crafting_recipe", "Investigation and Remedies",
         f"Something weighs on you? Use a **scrying glass** ({INVESTIGATE_FOCUS} Focus a look). The first look names each "
         "curse and its remedy; the second names who cast it, unless they are warded against scrying. Hold the remedy in "
         "your other hand and use the glass to lift the curse (the remedy is spent). With a taglock in your other hand the "
         "glass reads its link. **/jugcraft concordance hexes** lists what you know. Anyone may use a scrying glass.",
         rid("scrying_glass")),
        ("text", "Wards",
         "A ward sigil wards you against one kind of operation for twenty minutes (a fresh one renews it; wards never "
         "stack). Anyone may make and use one.\\\n" + "\\\n".join(ward_lines)),
    ]
    dreams = [
        ("text", "Dreamwalking",
         "Once you understand First Light, examine something from the edge of dreams (**sneak and use**): chorus fruit, an "
         "ender pearl, a white bed, a spore blossom. Study one at a Lampwright's Bench to understand Dreamwalking."),
        ("crafting_recipe", "The Oneiric Censer",
         f"Use the censer at night ({DREAM_FOCUS} Focus) to dream. Everything you carry is held for you; you dream in "
         f"adventure mode, with nothing, for at most {seconds(DREAM_TICKS)}, within {DREAM_RADIUS} blocks of your body. "
         f"Catch the dream wisps by walking into them: {DREAM_WISPS} gather at once and one more every "
         f"{seconds(WISP_TICKS)}; each is one dreamglass, at most {MAX_CAUGHT}. Use the censer again to wake.",
         rid("oneiric_censer")),
        ("text", "Waking",
         "However a dream ends, you wake with exactly what you carried, in the same slots, where your body lay, and "
         "never with more experience than you had (experience gained in the dream is gone; experience spent there, on an "
         "enchantment or a repair, stays spent). Anything you picked up while dreaming falls at your feet. You keep the "
         "dreamglass you caught; half if you strayed (left the dimension or went too far) or were hurt; none if you died "
         "(then your body dies with what it carried). Leaving the server mid-dream wakes you; a dream left open by a crash "
         "ends when you next join. Nothing worn in an accessory slot may come into a dream."),
    ]
    return {
        ("hexes", "sympathy"): {"name": "Sympathy", "x": 0, "y": 0, "icon": rid("taglock"), "condition": None,
                                "description": "Links, curses, investigation, remedies and wards", "pages": hexes},
        ("hexes", "dreamwalking"): {"name": "Dreamwalking", "x": 2, "y": 0, "icon": rid("oneiric_censer"), "condition": None,
                                    "description": "Dreams, and waking with all you had", "pages": dreams},
    }


CATEGORY = {"hexes": {"name": "Hexes and Dreams", "icon": rid("taglock"), "sort": 13,
                      "description": "Sympathy and its remedies, and dreams that give back all you had"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in SYMPATHY_SPECIMENS:
        tags.add("item", SYMPATHY_SPECIMEN_TAG, ref)
    for ref in DREAM_SPECIMENS:
        tags.add("item", DREAM_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("oneiric_censer"))


SHAPED = {
    "scrying_glass": {"pattern": [" A ", "GPG", " S "], "key": {"A": "minecraft:amethyst_shard", "G": "minecraft:gold_ingot",
                                                                 "P": "minecraft:glass_pane", "S": "minecraft:stick"}},
    "oneiric_censer": {"pattern": [" G ", "GMG", "SSS"], "key": {"G": "minecraft:gold_ingot", "M": "minecraft:phantom_membrane",
                                                                  "S": "minecraft:dark_oak_slab"}},
}
SHAPELESS = {
    "taglock": (["minecraft:glass_bottle", "minecraft:string", "minecraft:iron_nugget"], None),
    **{f"ward_sigil_{key}": (["minecraft:paper", rid("dreamglass"), ingredient], key) for key, (_n, _d, ingredient) in WARDS.items()},
}


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_hex_models as models
    write(assets / "geckolib" / "models" / "block" / "oneiric_censer.geo.json", models.GEO["oneiric_censer"]())
    write(assets / "geckolib" / "animations" / "block" / "oneiric_censer.animation.json", models.ANIMATIONS["oneiric_censer"]())
    write(assets / "geckolib" / "models" / "entity" / "dream_wisp.geo.json", models.GEO["dream_wisp"]())
    write(assets / "geckolib" / "animations" / "entity" / "dream_wisp.animation.json", models.ANIMATIONS["dream_wisp"]())
    write(assets / "blockstates" / "oneiric_censer.json", {"variants": {"": {"model": rid("block/oneiric_censer")}}})
    write(assets / "models" / "block" / "oneiric_censer.json", {"textures": {"particle": rid("block/oneiric_censer")}})
    for name in list(ITEMS) + list(BLOCKS):
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
        write(assets / "models" / "item" / f"{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    write(data / "loot_table" / "blocks" / "oneiric_censer.json", self_drop("oneiric_censer"))
    for name, recipe in SHAPED.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": recipe["pattern"], "key": recipe["key"],
            "result": {"id": rid(name), "count": 1}}))
    for name, (ingredients, ward) in SHAPELESS.items():
        result = {"id": rid("ward_sigil" if ward else name), "count": 1}
        if ward:
            result["components"] = {rid("ward"): ward}
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients, "result": result}))


def write_data(write, data):
    for key, info in CURSES.items():
        write(data / "concordance" / "curse" / f"{key}.json",
              {"schema": 1, **{field: info[field] for field in ("reagent", "status", "amplifier", "pulse_duration", "pulse_ticks",
                                                                 "total_ticks", "strength", "focus", "remedy")}})
