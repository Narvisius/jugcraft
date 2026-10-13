"""Roadmap step 17: spirits, familiars and constructs (docs/features/arcane-concordance-workers.md).

Three separate models, each owned by the server and saved with its worker, never in its brain:
- a familiar (the Hearthling) keeps close to its one person and mends them by its bond, which grows by time spent
  together (capped a day) and fades when neglected;
- a spirit (the Gathering Shade) works only within an agreement sealed at its Spirit Anchor as a Bound Will: one kind
  of work, an area, hours by the world clock and a daily quota; its holder may suspend or release it;
- a construct (the Clockwork Porter) carries what its body allows between two containers, spends Ley Charge each trip,
  wears and is repaired with copper.

Every worker always says what it is doing or exactly why not (waiting for resources, blocked by access, unable to
navigate, outside its agreement, finished, owner offline, another dimension, destination unloaded, suspended, needs
repair, no energy, full). None loads a chunk. The definitions are data (data/jugcraft/concordance/worker, read by
Java's concordance/worker). tools/concordance.py merges these tables into its own; tools/check_mod_data.py checks the
numbers the Java repeats.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Shared (Java: spirits/WorkerEntity.java, worker/Navigation.java).
THINK_TICKS = 20
GIVE_UP = 3
RETRY_TICKS = 600
# The familiar's bond (Java: worker/Bond.java).
BOND_MAX = 100
BOND_GAIN = 1
BOND_DAILY_GAIN = 24
BOND_NEGLECT = 4
BOND_FIRST = 20
BOND_SECOND = 60
VISIT_TICKS = 200  # Java: spirits/HearthlingEntity.java
# Parser limits (Java: worker/WorkerParser.java).
MAX_RADIUS = 12
MAX_CARRY = 64
MAX_QUOTA = 256
MAX_SUPPORT_TICKS = 200
WORK = ["gather"]
# The roster (Java: spirits/WorkerRoster.java).
ROSTER_MAX = 32

STATUSES = {
    "following": "following its person",
    "supporting": "mending its person",
    "travelling": "on its way",
    "working": "working",
    "returning": "taking what it carries home",
    "idle": "idle: nothing to do",
    "waiting_for_resources": "waiting for resources (its source is empty or gone)",
    "blocked_by_access": "blocked: its keeper may not take from or give to that place",
    "cannot_navigate": "cannot find a way there",
    "outside_agreement": "outside its agreement (the hours, the area or the work)",
    "finished": "finished: its work for the day is done",
    "owner_offline": "waiting: its person is not here",
    "other_dimension": "waiting: its person or route is in another dimension",
    "destination_unloaded": "waiting: its destination is not loaded",
    "suspended": "suspended by its holder",
    "needs_repair": "needs repair (copper)",
    "no_energy": "out of Ley Charge (a pylon by its source fuels it)",
    "full": "full: there is no room to deliver",
    "disabled": "switched off",
}

BINDING_SPECIMEN_TAG = f"{MOD}:binding_specimens"
BINDING_SPECIMENS = {
    "minecraft:lead": "Lead", "minecraft:name_tag": "Name Tag", "minecraft:soul_lantern": "Soul Lantern",
    "minecraft:saddle": "Saddle", "minecraft:echo_shard": "Echo Shard",
}
SERVICE = rid("worker_service")  # Java: Workers.ACTIVITY

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "binding_arts": {
        "name": "Binding Arts",
        "principle": "tether",
        "tradition": "spiritbinders",
        "stage": "practitioner",
        "icon": rid("bonding_charm"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{BINDING_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{BINDING_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{BINDING_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: a familiar's mending, a spirit's delivery and a construct's trip, each once.
            "mastered": [{"type": "practice", "activity": SERVICE, "distinct": 3}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Tethered", "Examine something that binds", "task", "minecraft:lead"),
            "observed": ("Terms and Conditions", "Examine three different things that bind", "task", "minecraft:name_tag"),
            "understood": ("Binding Arts", "Understand the Binding Arts: study something that binds at a Lampwright's Bench",
                           "goal", rid("bonding_charm")),
            "mastered": ("Keeper of Many", "Be served by a familiar, a spirit and a construct", "challenge", rid("spirit_anchor")),
        },
        "locked": {"encountered": "Examine something that binds first",
                   "observed": "Examine three different things that bind first",
                   "understood": "Understand the Binding Arts first", "mastered": "Master the Binding Arts first"},
    },
}

# ------------------------------------------------------------------------------------------------- definitions

WORKERS = {
    "hearthling": {"kind": "familiar", "follow": 8,
                   "support": {"effect": "minecraft:regeneration", "amplifier": 0, "duration": 100}},
    "gathering_shade": {"kind": "spirit", "work": "gather", "radius": 8, "from": 13000, "to": 23000, "quota": 64,
                        "carry": 16},
    "clockwork_porter": {"kind": "construct", "integrity": 64, "wear": 1,
                         "repair": {"item": "minecraft:copper_ingot", "amount": 16}, "energy": 64, "trip_energy": 2,
                         "carry": 16},
}
ENTITY_NAMES = {"hearthling": "Hearthling", "gathering_shade": "Gathering Shade", "clockwork_porter": "Clockwork Porter"}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "bonding_charm": {"name": "Bonding Charm",
                      "tooltip": "Use: bind a Hearthling, or call yours to you. Sneak: release it."},
    "clockwork_porter": {"name": "Clockwork Porter",
                         "tooltip": "Use on a block: unfold a porter. Give it a route with a Porter Key."},
    "porter_key": {"name": "Porter Key",
                   "tooltip": "Use on a container: the source, then the target. Use on a porter: give it the route."},
}
BLOCKS = {"spirit_anchor": {"name": "Spirit Anchor"}}
DEVICE_TOOLTIPS = {
    "spirit_anchor": "Use: seal a Gathering Shade's agreement. Its holder, sneaking: suspend or resume. Break: release.",
}

MESSAGES = {
    "workers.unknown": "You have not understood the Binding Arts (the codex says how)",
    "workers.status": "%s: %s",
    "workers.familiar": "%s; bond %s / %s",
    "workers.spirit": "%s; carrying %s",
    "workers.construct": "%s; integrity %s / %s, Ley Charge %s / %s, carrying %s",
    "workers.bound": "A Hearthling answers your charm",
    "workers.no_familiar": "You have no familiar to release",
    "workers.released": "Your familiar is released",
    "workers.not_here": "Your familiar is not near (another dimension, or not loaded)",
    "workers.recalled": "Your familiar comes to you",
    "workers.sealed": "Sealed: a Gathering Shade will gather within %s blocks, from %s to %s, up to %s times a day",
    "workers.suspended": "The agreement is suspended",
    "workers.resumed": "The agreement is resumed",
    "workers.agreement": "Agreement: within %s blocks, %s to %s; %s of %s tasks today; %s",
    "workers.is_suspended": "suspended",
    "workers.is_active": "active",
    "workers.spirit_away": "Its spirit is not loaded here",
    "workers.key_source": "Source: %s %s %s (now use it on the target)",
    "workers.key_target": "Target: %s %s %s (now use it on a porter)",
    "workers.key_unset": "The key has no route yet (use it on two containers)",
    "workers.key_refused": "You may not use that container yourself, so no porter may take or bring for you there",
    "workers.not_yours": "Only its keeper (or their party) may command it",
    "workers.routed": "The porter takes the route",
    "workers.none": "You have no workers",
    "workers.line": "%s: %s (%s, %s)",
    "workers.kind.familiar": "Familiar",
    "workers.kind.spirit": "Spirit",
    "workers.kind.construct": "Construct",
}

TOOLTIPS = {
    "binding_specimen": "Something that binds: sneak and use to examine it",
    "porter_key.route": "Route: %s to %s",
    "jade.bond": "Bond: %s / %s",
    "jade.body": "Integrity %s / %s, Ley Charge %s / %s",
    "jade.carried": "Carrying %s",
}


def lang_entries(lang):
    lang[f"tag.item.{BINDING_SPECIMEN_TAG.replace(':', '.')}"] = "Things That Bind"
    for key, text in STATUSES.items():
        lang[f"compose.{MOD}.worker.status.{key}"] = text
    for key, name in ENTITY_NAMES.items():
        lang[f"entity.{MOD}.{key}"] = name
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"config.jade.plugin_{MOD}.worker"] = "Familiars, spirits and constructs"


# ------------------------------------------------------------------------------------------------- codex

def clock(tick):
    minutes = (tick * 60 // 1000 + 6 * 60) % (24 * 60)
    return f"{minutes // 60:02d}:{minutes % 60:02d}"


def codex():
    art = "binding_arts"
    familiar, spirit, construct = WORKERS["hearthling"], WORKERS["gathering_shade"], WORKERS["clockwork_porter"]
    specimens = "\\\n".join(f"- {name}" for name in BINDING_SPECIMENS.values())
    reasons = "\\\n".join(f"- **{key.replace('_', ' ')}**: {text}" for key, text in STATUSES.items())
    return {
        ("binding", "binding_arts"): {
            "name": "Binding Arts", "x": 0, "y": 0, "icon": rid("bonding_charm"), "condition": None,
            "description": "Familiars, spirits and constructs: research",
            "pages": [
                ("text", "Binding Arts",
                 "Once you understand First Light, look closely at what binds one thing to another. **Sneak and use** "
                 "one of these to examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 "A familiar, a spirit and a construct will answer you now. To **master** the Binding Arts, be served "
                 "by each once: a familiar's mending, a spirit's delivery and a construct's trip.", (art, "understood")),
                ("text", "Mastered", "You keep many, and each keeps its own terms.", (art, "mastered")),
            ],
        },
        ("binding", "familiars"): {
            "name": "Familiars", "x": 2, "y": 0, "icon": rid("bonding_charm"), "condition": (art, "observed"),
            "description": "A Hearthling and its bond",
            "pages": [
                ("crafting_recipe", "Bonding Charm",
                 f"Use it to bind a **Hearthling**: one familiar a person. It keeps within {familiar['follow']} blocks "
                 f"of you and, when you are badly hurt, mends you. Use the charm again to call it to your side; sneak "
                 f"to release it.", rid("bonding_charm")),
                ("text", "The Bond",
                 f"The bond grows by time spent together, {BOND_GAIN} a visit and at most {BOND_DAILY_GAIN} a day, and "
                 f"fades by {BOND_NEGLECT} for each day you are online but away from it. At {BOND_FIRST} it mends you "
                 f"(at most once a minute); at {BOND_SECOND}, twice as often. It never follows you into another "
                 f"dimension and never waits in a place that is not loaded: it simply waits where it is."),
            ],
        },
        ("binding", "spirits"): {
            "name": "Spirits", "x": 4, "y": 0, "icon": rid("spirit_anchor"), "condition": (art, "understood"),
            "description": "Agreements with a Gathering Shade",
            "pages": [
                ("crafting_recipe", "Spirit Anchor",
                 f"Use it to seal an **agreement** with a Gathering Shade, recorded as a Bound Will: it gathers dropped "
                 f"items within {spirit['radius']} blocks of the anchor, from {clock(spirit['from'])} to "
                 f"{clock(spirit['to'])}, at most {spirit['quota']} times a day, carrying {spirit['carry']} at a time, "
                 f"and brings them to the anchor (hoppers below take them out).", rid("spirit_anchor")),
                ("text", "Its Terms",
                 "Outside its hours, its area or its work it waits at the anchor and says so; when its day's work is "
                 "done it is finished until tomorrow. It never takes what you could not take yourself (a protected "
                 "town). Sneak and use the anchor to **suspend** or resume it; break the anchor to **release** it: it "
                 "leaves what it carried and departs."),
            ],
        },
        ("binding", "constructs"): {
            "name": "Constructs", "x": 2, "y": 2, "icon": rid("clockwork_porter"), "condition": (art, "understood"),
            "description": "A Clockwork Porter's body",
            "pages": [
                ("crafting_recipe", "Clockwork Porter",
                 f"Unfold it on a block. Use a **Porter Key** on two containers (its source and its target), then on "
                 f"the porter. It carries {construct['carry']} items a trip, spends {construct['trip_energy']} Ley "
                 f"Charge a trip (it holds {construct['energy']}, drawn from a Ley Pylon by its source that you may "
                 f"use) and wears {construct['wear']} of its {construct['integrity']} integrity; each copper ingot "
                 f"mends {construct['repair']['amount']}.", rid("clockwork_porter")),
                ("crafting_recipe", "Porter Key", "Sets a porter's route.", rid("porter_key")),
            ],
        },
        ("binding", "reasons"): {
            "name": "Why It Waits", "x": 4, "y": 2, "icon": "minecraft:clock", "condition": (art, "encountered"),
            "description": "What a worker can tell you",
            "pages": [
                ("text", "Why It Waits",
                 "Look at a worker (Jade), use it with an empty hand, or ask **/jugcraft concordance workers** "
                 "(which also lists workers that are not loaded, with what they last said):\\\n" + reasons),
            ],
        },
    }


CATEGORY = {"binding": {"name": "Binding", "icon": rid("bonding_charm"), "sort": 9,
                        "description": "Familiars, spirits and constructs: bonds, agreements and bodies"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in BINDING_SPECIMENS:
        tags.add("item", BINDING_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("spirit_anchor"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_worker_models as models
    for key in ENTITY_NAMES:
        write(assets / "geckolib" / "models" / "entity" / f"{key}.geo.json", models.GEO[key]())
        write(assets / "geckolib" / "animations" / "entity" / f"{key}.animation.json", models.ANIMATIONS[key]())
    write(assets / "models" / "block" / "spirit_anchor.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": rid("block/spirit_anchor_top"), "side": rid("block/spirit_anchor_side"),
                     "bottom": rid("block/spirit_anchor_top")}})
    write(assets / "blockstates" / "spirit_anchor.json", {"variants": {"": {"model": rid("block/spirit_anchor")}}})
    write(assets / "items" / "spirit_anchor.json", {"model": {"type": "minecraft:model", "model": rid("item/spirit_anchor")}})
    write(assets / "models" / "item" / "spirit_anchor.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid("item/spirit_anchor")}})
    write(data / "loot_table" / "blocks" / "spirit_anchor.json", self_drop("spirit_anchor"))
    for item in ITEMS:
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    feature = "concordance"
    recipes = {
        "bonding_charm": {"type": "minecraft:crafting_shapeless", "category": "misc",
                          "ingredients": ["minecraft:string", "minecraft:gold_ingot", "minecraft:glow_berries",
                                          "minecraft:amethyst_shard"],
                          "result": {"id": rid("bonding_charm"), "count": 1}},
        "spirit_anchor": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["DAD", "DLD", "DDD"],
                          "key": {"D": "minecraft:polished_deepslate", "A": "minecraft:amethyst_shard",
                                  "L": "minecraft:soul_lantern"},
                          "result": {"id": rid("spirit_anchor"), "count": 1}},
        "clockwork_porter": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["CBC", "CRC", "I I"],
                             "key": {"C": "minecraft:copper_ingot", "B": "minecraft:barrel", "R": "minecraft:redstone",
                                     "I": "minecraft:iron_ingot"},
                             "result": {"id": rid("clockwork_porter"), "count": 1}},
        "porter_key": {"type": "minecraft:crafting_shapeless", "category": "misc",
                       "ingredients": ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:iron_nugget"],
                       "result": {"id": rid("porter_key"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key, info in WORKERS.items():
        write(data / "concordance" / "worker" / f"{key}.json", dict({"schema": 1}, **info))
