"""Roadmap step 20: relics and shrine behaviour (docs/features/arcane-concordance-relics.md).

Four relics, each with its own purpose and explicit activation contexts: the Wardlight Lantern (held in the off hand,
or installed: reveals hostile creatures), the Hearthstone (worn as a necklace in a Trinkets slot, or installed: mends;
it binds to the first player it serves), the Stormglass Orb (held in the main hand under the open sky: speed; it
cannot be installed) and the Owlsight Circlet (worn on the head in the dark: night vision). Carried loose in an
inventory, worn for show in a cosmetic slot, kept in a chest or held in the wrong hand, a relic does nothing, and it
says why. A Reliquary Shrine holds one installed relic and recharges relics from the Ley Pylons beside it.

The definitions are data (data/jugcraft/concordance/relic, read by Java's concordance/relic; the server side is
concordance/reliquary). tools/concordance.py merges these tables into its own; tools/check_mod_data.py checks the
numbers the Java repeats.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Java: relic/Relics.java, relic/RelicParser.java.
BUDGET = 2
SHRINE_BUDGET = 4
BUDGET_TICKS = 20
CALM_TICKS = 200
MAX_CAPACITY = 256
MAX_RANGE = 32
MIN_INTERVAL = 20
MAX_INTERVAL = 1200
MAX_DURATION = 600
MAX_AMPLIFIER = 1
# The server side (Java: reliquary/Reliquary.java).
CHECK_TICKS = 20
CHARGE_PER_LEY = 4
RECHARGE_PER_SECOND = 8
PYLON_REACH = 2
MAX_TARGETS = 12

# The statuses a relic may give (tools/check_mod_data.py holds every mode to these): mild ones only.
RELIC_EFFECTS = {"minecraft:glowing", "minecraft:regeneration", "minecraft:speed", "minecraft:haste", "minecraft:night_vision",
                 "minecraft:water_breathing", "minecraft:fire_resistance", "minecraft:slow_falling"}

CONTEXTS = {
    "main_hand": "held in the main hand",
    "off_hand": "held in the off hand",
    "head": "worn on the head",
    "chest": "worn on the chest",
    "legs": "worn on the legs",
    "feet": "worn on the feet",
    "trinket": "worn in an accessory slot",
    "installed": "installed in a Reliquary Shrine",
    "inventory": "carried loose",
    "cosmetic": "worn for show in a cosmetic slot",
}
REASONS = {
    "wrong_context": "it works only when %s",
    "cannot_install": "it cannot be installed; it works only when %s",
    "not_owner": "it is bound to someone else",
    "wrong_dimension": "it does not work in this dimension",
    "needs_sky": "it needs the open sky above",
    "needs_night": "it works only at night",
    "not_calm": "it works only when you have not been hurt for ten seconds",
    "not_wounded": "it mends only when you are hurt",
    "no_charge": "it is out of charge (a Reliquary Shrine by a Ley Pylon recharges it)",
    "resting": "it is gathering itself for its next pulse",
    "budget": "your other relics are using this second's pulses; it tries again next second",
    "no_target": "nothing within its reach needs it",
    "kept": "a stronger effect already holds wherever it reaches",
    "unknown": "it has no definition in the Concordance's data",
    "disabled": "the Arcane Concordance is disabled on this server",
}

# ------------------------------------------------------------------------------------------------- definitions

def mode(mode_id, contexts, target, status, duration, interval, cost, rng=0, amplifier=0, sky=False, night=False, calm=False,
         wounded=False, dimensions=()):
    return {"id": mode_id, "contexts": list(contexts), "target": target, "status": status, "amplifier": amplifier,
            "duration": duration, "range": rng, "interval": interval, "cost": cost,
            "needs": {"sky": sky, "night": night, "calm": calm, "wounded": wounded, "dimensions": list(dimensions)}}


RELICS = {
    "wardlight": {"name": "Wardlight Lantern", "capacity": 64, "owned": False,
                  "tooltip": "A relic. Held in the off hand, or installed in a Reliquary Shrine: hostile creatures nearby glow.",
                  "modes": [mode("reveal", ["off_hand"], "hostiles", "minecraft:glowing", 60, 40, 1, rng=12),
                            mode("watch", ["installed"], "hostiles", "minecraft:glowing", 60, 40, 1, rng=24)]},
    "hearthstone": {"name": "Hearthstone", "capacity": 48, "owned": True,
                    "tooltip": "A relic that binds to the first who uses it. Worn as a necklace in an accessory slot, or "
                               "installed in a Reliquary Shrine: it mends.",
                    "modes": [mode("mend", ["trinket"], "self", "minecraft:regeneration", 100, 200, 2, calm=True, wounded=True),
                              mode("hearth", ["installed"], "allies", "minecraft:regeneration", 100, 200, 1, rng=8, wounded=True)]},
    "stormglass": {"name": "Stormglass Orb", "capacity": 32, "owned": False,
                   "tooltip": "A relic. Held in the main hand under the open sky, it quickens your step. It cannot be "
                              "installed.",
                   "modes": [mode("quicken", ["main_hand"], "self", "minecraft:speed", 60, 40, 1, sky=True)]},
    "owlsight_circlet": {"name": "Owlsight Circlet", "capacity": 32, "owned": False,
                         "tooltip": "A relic. Worn on the head in the dark of night, it lets you see.",
                         "modes": [mode("owlsight", ["head"], "self", "minecraft:night_vision", 440, 200, 1, night=True)]},
}
# Which Trinkets slot a worn relic goes in (data/trinkets/tags/item/<group>/<slot>.json), and which players get it.
TRINKET_SLOTS = {"hearthstone": "chest/necklace"}

RELIC_SPECIMEN_TAG = f"{MOD}:relic_specimens"
RELIC_SPECIMENS = {
    "minecraft:heart_of_the_sea": "Heart of the Sea", "minecraft:nautilus_shell": "Nautilus Shell",
    "minecraft:totem_of_undying": "Totem of Undying", "minecraft:ender_eye": "Eye of Ender",
    "minecraft:recovery_compass": "Recovery Compass",
}
RELIC_PRACTICE = rid("relic_pulse")  # Java: Reliquary.ACTIVITY
RELIC_IDS = list(RELICS)  # Java: Reliquary.RELIC_IDS (the item ids, in their order)

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "relic_lore": {
        "name": "Relic Lore",
        "principle": "echo",
        "tradition": "runesmiths",
        "stage": "practitioner",
        "icon": rid("wardlight"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{RELIC_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{RELIC_SPECIMEN_TAG}", "distinct": 2}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{RELIC_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: relics serving in three different contexts.
            "mastered": [{"type": "practice", "activity": RELIC_PRACTICE, "distinct": 3}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Old Things", "Examine something that has outlived its maker", "task", "minecraft:nautilus_shell"),
            "observed": ("Provenance", "Examine two different relic-like things", "task", "minecraft:heart_of_the_sea"),
            "understood": ("Relic Lore", "Understand Relic Lore: study something old at a Lampwright's Bench", "goal", rid("wardlight")),
            "mastered": ("Keeper of Relics", "Have relics serve you in three different ways", "challenge", rid("reliquary_shrine")),
        },
        "locked": {"encountered": "Examine something that has outlived its maker first",
                   "observed": "Examine two different relic-like things first",
                   "understood": "Understand Relic Lore first", "mastered": "Master Relic Lore first"},
    },
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {key: {"name": info["name"], "tooltip": info["tooltip"]} for key, info in RELICS.items()}
BLOCKS = {"reliquary_shrine": {"name": "Reliquary Shrine"}}
DEVICE_TOOLTIPS = {
    "reliquary_shrine": "Use with a relic: install it, or recharge it from a Ley Pylon beside the shrine. Empty hand: take it back.",
}

MESSAGES = {
    "relic.status": "%s, %s: %s",
    "relic.working": "working (%s, charge %s / %s)",
    "relic.cannot": "does nothing: %s",
    "relic.none": "You carry no relics",
    "relic.installed": "%s installed",
    "relic.taken": "%s taken back",
    "relic.recharged": "%s recharged: %s / %s",
    "relic.full": "%s is fully charged",
    "relic.shrine_empty": "The shrine is empty: use an installable relic on it to install it",
    "relic.not_yours": "Only the shrine's owner (or their party) may use it",
    "relic.unknown": "You have not understood Relic Lore (the codex says how)",
    "relic.no_pylon": "No Ley Pylon with charge (that lends to you) stands within 2 blocks of the shrine",
}
TOOLTIPS = {
    "relic_specimen": "Something old: sneak and use to examine it",
    "relic.charge": "Charge: %s",
    "relic.bound": "Bound to its player",
    "jade.shrine": "%s: %s",
    "jade.shrine_working": "working (%s)",
    "jade.shrine_charge": "Charge %s / %s",
    "jade.shrine_empty": "Empty: use an installable relic on it",
}


def lang_entries(lang):
    lang[f"tag.item.{RELIC_SPECIMEN_TAG.replace(':', '.')}"] = "Relic-like Things"
    lang[f"compose.{MOD}.relic.or"] = "%s or %s"
    for key, text in CONTEXTS.items():
        lang[f"compose.{MOD}.relic.context.{key}"] = text
    for key, text in REASONS.items():
        lang[f"compose.{MOD}.relic.reason.{key}"] = text
    for key, info in RELICS.items():
        for m in info["modes"]:
            lang[f"compose.{MOD}.relic.mode.{m['id']}"] = m["id"].replace("_", " ")
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"config.jade.plugin_{MOD}.reliquary_shrine"] = BLOCKS["reliquary_shrine"]["name"]


# ------------------------------------------------------------------------------------------------- codex

def seconds(ticks):
    return f"{ticks / 20:g} s"


def codex():
    pages = [("text", "Relic Lore",
              "Once you understand First Light, examine something that has outlived its maker (**sneak and use**):\\\n"
              + "\\\n".join(f"- {name}" for name in RELIC_SPECIMENS.values())
              + "\\\nStudy one at a Lampwright's Bench to understand Relic Lore."),
             ("text", "Where Relics Work",
              "A relic works only where it is meant to: **held** in a particular hand, **worn** in an armour slot or an "
              "accessory (Trinkets) slot, or **installed** in a Reliquary Shrine. Each relic names its places.\\\n"
              "Anywhere else it does nothing: carried loose in your inventory, worn for show in a cosmetic slot, held in "
              "the other hand, or kept in a chest, a shulker box or a bundle (nothing inside those is ever looked at).\\\n"
              "**/jugcraft concordance relics** lists the relics you carry, where each one is, and whether it works or "
              "exactly why not.")]
    for key, info in RELICS.items():
        lines = []
        for m in info["modes"]:
            where = " or ".join(CONTEXTS[c] for c in m["contexts"])
            target = {"self": "you", "allies": f"your party within {m['range']} blocks",
                      "hostiles": f"hostile creatures within {m['range']} blocks"}[m["target"]]
            needs = [text for flag, text in (("sky", "the open sky above"), ("night", "the dark of night"),
                                             ("calm", "ten calm seconds"), ("wounded", "someone hurt"))
                     if m["needs"][flag]]
            lines.append(f"- **{where}**: {m['status'].split(':')[1].replace('_', ' ')} on {target} for "
                         f"{seconds(m['duration'])}, every {seconds(m['interval'])}, {m['cost']} charge a pulse"
                         + (f"; needs {', '.join(needs)}" if needs else ""))
        if not any("installed" in m["contexts"] for m in info["modes"]):
            lines.append("- cannot be installed")
        lines.append("- anywhere else it does nothing, and says why")
        if info["owned"]:
            lines.append("- binds to the first player it serves, and then serves only them")
        pages.append(("crafting_recipe", info["name"], f"Holds {info['capacity']} charge.\\\n" + "\\\n".join(lines), rid(key)))
    pages.append(("crafting_recipe", "Reliquary Shrine",
                  f"Use a relic on an empty shrine to **install** it, if it has an installed way of working. Use any "
                  f"other relic on it (or one that cannot be installed) to **recharge** it: each Ley Charge from the Ley "
                  f"Pylons within {PYLON_REACH} blocks gives {CHARGE_PER_LEY} charge. An installed relic is charged from "
                  f"those pylons as it works, {RECHARGE_PER_SECOND} a second. Use the shrine with an empty hand to take "
                  f"its relic back. Only its owner and their party may use it.", rid("reliquary_shrine")))
    pages.append(("text", "Many Relics",
                  f"However many relics you carry and wear, they pulse at most {BUDGET} times a second together; one "
                  f"owner's shrines, at most {SHRINE_BUDGET}. A relic held back waits a second and is not spent. A relic "
                  f"spends charge only when its pulse changes something, and two relics giving the same effect never add "
                  f"up: the stronger holds."))
    return {
        ("relics", "relic_lore"): {
            "name": "Relic Lore", "x": 0, "y": 0, "icon": rid("wardlight"), "condition": None,
            "description": "Relics, where they work, and the Reliquary Shrine",
            "pages": pages,
        },
    }


CATEGORY = {"relics": {"name": "Relics", "icon": rid("wardlight"), "sort": 11,
                       "description": "Relics that work where they are meant to, and say why not elsewhere"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in RELIC_SPECIMENS:
        tags.add("item", RELIC_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("reliquary_shrine"))


RECIPES = {
    "wardlight": {"pattern": ["GAG", "ALA", "GAG"], "key": {"G": "minecraft:gold_nugget", "A": "minecraft:amethyst_shard",
                                                              "L": "minecraft:lantern"}},
    "hearthstone": {"pattern": [" S ", "SHS", " B "], "key": {"S": "minecraft:string", "H": "minecraft:heart_of_the_sea",
                                                                "B": "minecraft:blaze_powder"}},
    "stormglass": {"pattern": ["PGP", "GCG", "PGP"], "key": {"P": "minecraft:prismarine_shard", "G": "minecraft:glass",
                                                               "C": "minecraft:lightning_rod"}},
    "owlsight_circlet": {"pattern": ["GEG", "G G"], "key": {"G": "minecraft:gold_ingot", "E": "minecraft:ender_eye"}},
    "reliquary_shrine": {"pattern": ["CAC", "SBS", "SSS"], "key": {"C": "minecraft:candle", "A": "minecraft:amethyst_block",
                                                                     "S": "minecraft:polished_blackstone", "B": "minecraft:lodestone"}},
}


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    import concordance_relic_models as models
    write(assets / "geckolib" / "models" / "block" / "reliquary_shrine.geo.json", models.GEO["reliquary_shrine"]())
    write(assets / "geckolib" / "animations" / "block" / "reliquary_shrine.animation.json", models.ANIMATIONS["reliquary_shrine"]())
    write(assets / "blockstates" / "reliquary_shrine.json", {"variants": {"": {"model": rid("block/reliquary_shrine")}}})
    # The block model only gives the particle; GeckoLib draws the shrine.
    write(assets / "models" / "block" / "reliquary_shrine.json", {"textures": {"particle": rid("block/reliquary_shrine")}})
    for name in RELIC_IDS + ["reliquary_shrine"]:
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
        write(assets / "models" / "item" / f"{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    # The circlet is drawn on the head when worn (Java: its equippable component names this asset).
    write(assets / "equipment" / "owlsight_circlet.json", {"layers": {"humanoid": [{"texture": rid("owlsight_circlet")}]}})
    write(data / "loot_table" / "blocks" / "reliquary_shrine.json", self_drop("reliquary_shrine"))
    for name, recipe in RECIPES.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition("concordance")}, **{
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": recipe["pattern"], "key": recipe["key"],
            "result": {"id": rid(name), "count": 1}}))
    for relic, slot in TRINKET_SLOTS.items():
        group, name = slot.split("/")
        write(data.parent / "trinkets" / "tags" / "item" / group / f"{name}.json", {"replace": False, "values": [rid(relic)]})
    # Trinkets merges every file here: this one gives players the necklace slot (step 19's gives the ring slot).
    write(data.parent / "trinkets" / "entities" / f"{MOD}_relics.json", {"entities": ["player"], "slots": sorted(set(TRINKET_SLOTS.values()))})
    # The wardlight glows in hand where LambDynamicLights is installed (client light only; it changes nothing on the server).
    write(assets / "dynamiclights" / "item" / "wardlight.json", {"match": {"items": rid("wardlight")}, "luminance": 10})


def write_data(write, data):
    for key, info in RELICS.items():
        write(data / "concordance" / "relic" / f"{key}.json",
              {"schema": 1, "item": rid(key), "capacity": info["capacity"], "owned": info["owned"], "modes": info["modes"]})
