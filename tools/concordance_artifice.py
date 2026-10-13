"""Roadmap step 19: equipment construction and enhancement (docs/features/arcane-concordance-artifice.md).

Runesmithing. A Resonant Ring is forged at the Artificer's Bench from a substrate (copper, iron, gold or netherite);
the server rolls its craft quality and affixes from a seed it saves on the ring before anyone sees them. A ring has a
resonance capacity (its substrate's, plus its quality's bonus, plus one while bonded) that its affixes, runes and gems
use; nothing is added beyond it. Every process keeps and destroys exactly what the rules say (Java: artifice/Forge.java,
Forge.Process), and reforging rolls from the ring's own seed and reforge count, so previewing, cancelling, reconnecting
or reopening never rerolls. Salvage gives back less substrate than forging costs, and every socketed gem.

Rings are Trinkets Updated rings (the hand/ring slot): Trinkets applies and removes their modifiers once on equip and
unequip. Their statistics are vanilla attributes and Spell Power's (spell_power:arcane), which Spell Engine's casting
reads. The definitions are data (data/jugcraft/concordance/{substrate,gem,rune,affix}, read by Java's
concordance/artifice). tools/concordance.py merges these tables into its own; tools/check_mod_data.py checks the
numbers the Java repeats.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# Java: artifice/Forge.java, artifice/Quality.java, artifice/ArtificeParser.java.
BOND_CAPACITY = 1
QUALITIES = {"crude": (-1, 1, 15), "sound": (0, 1, 55), "fine": (1, 2, 24), "masterwork": (2, 3, 6)}  # bonus, affixes, weight
MAX_COST = 9
MAX_CAPACITY = 12
MAX_SOCKETS = 3
MAX_RUNES = 3
MAX_PART_COST = 6
# The bench (Java: smithy/Artificery.java).
FORGE_FOCUS = 8
REFORGE_FOCUS = 6
INSCRIBE_FOCUS = 4
BOND_FOCUS = 10
UNSOCKET_TOOL = "minecraft:shears"
SALVAGE_TOOL = "minecraft:flint"
BOND_TOOL = "minecraft:lead"
REFORGE_CATALYST = "minecraft:redstone"
CONFIRM_TICKS = 200
WEAR_TICKS = 600  # a worn ring wears one point every 30 seconds; at its last point it is dull (gives nothing) until repaired

PROCESSES = {
    "forge": ("Forge", "a new ring: substrate, quality and affixes rolled once"),
    "reforge": ("Reforge", "keeps substrate, quality, runes, gems, bond and wear; new affixes"),
    "inscribe": ("Inscribe", "keeps everything; adds a rune"),
    "socket": ("Socket", "keeps everything; sets a gem"),
    "unsocket": ("Unsocket", "keeps everything but the last gem, which comes back whole"),
    "bond": ("Bond", "keeps everything; the ring serves only you, with one more capacity"),
    "unbond": ("Unbond", "keeps everything but the bond"),
    "repair": ("Repair", "keeps everything; mends wear"),
    "salvage": ("Salvage", "gives back some substrate and every gem; destroys quality, affixes, runes and bond"),
}
REFUSALS = {
    "incompatible": "That rune cannot be inscribed on this substrate",
    "already_inscribed": "That rune is already inscribed",
    "no_rune_slot": "No rune slot is left",
    "no_socket": "No socket is left",
    "over_capacity": "Not enough resonance capacity",
    "no_gem": "No gem is socketed",
    "already_bonded": "It is already bonded to you",
    "bonded_to_another": "It is bonded to someone else",
    "not_bonded": "It is not bonded",
    "no_focus": "Not enough Focus",
    "not_enough": "Not enough of its substrate",
    "nothing_to_repair": "It is not worn",
    "unknown": "You have not understood Runesmithing (the codex says how)",
}

ARTIFICE_SPECIMEN_TAG = f"{MOD}:artifice_specimens"
ARTIFICE_SPECIMENS = {
    "minecraft:flint": "Flint", "minecraft:gold_nugget": "Gold Nugget", "minecraft:iron_nugget": "Iron Nugget",
    "minecraft:lapis_lazuli": "Lapis Lazuli", "minecraft:quartz": "Nether Quartz",
}
ARTIFICE_PRACTICE = rid("artifice")  # Java: Artificery.ACTIVITY

# ------------------------------------------------------------------------------------------------- definitions

SUBSTRATES = {
    "copper": {"name": "Copper", "item": "minecraft:copper_ingot", "cost": 4, "salvage": 2, "capacity": 4, "sockets": 1, "runes": 1,
               "durability": 160, "repair": 40, "affixes": ["vitality", "fortune", "swiftness", "warding"]},
    "iron": {"name": "Iron", "item": "minecraft:iron_ingot", "cost": 4, "salvage": 2, "capacity": 5, "sockets": 1, "runes": 1,
             "durability": 250, "repair": 60, "affixes": ["vitality", "warding", "might", "fortune"]},
    "gold": {"name": "Gold", "item": "minecraft:gold_ingot", "cost": 4, "salvage": 2, "capacity": 7, "sockets": 2, "runes": 2,
             "durability": 120, "repair": 30, "affixes": ["arcane_power", "fortune", "vitality", "swiftness"]},
    "netherite": {"name": "Netherite", "item": "minecraft:netherite_ingot", "cost": 1, "salvage": 0, "capacity": 9, "sockets": 3,
                  "runes": 2, "durability": 600, "repair": 150,
                  "affixes": ["arcane_power", "vitality", "warding", "might", "swiftness", "fortune"]},
}
AFFIXES = {
    "arcane_power": {"name": "Arcane Power", "attribute": "spell_power:arcane", "operation": "add_value", "min": 0.5, "max": 2.0,
                     "steps": 3, "cost": 2, "group": "arcane"},
    "vitality": {"name": "Vitality", "attribute": "minecraft:max_health", "operation": "add_value", "min": 1.0, "max": 4.0, "steps": 3,
                 "cost": 2, "group": "vitality"},
    "fortune": {"name": "Fortune", "attribute": "minecraft:luck", "operation": "add_value", "min": 0.5, "max": 2.0, "steps": 3,
                "cost": 1, "group": "fortune"},
    "swiftness": {"name": "Swiftness", "attribute": "minecraft:movement_speed", "operation": "add_multiplied_base", "min": 0.02,
                  "max": 0.06, "steps": 2, "cost": 2, "group": "swiftness"},
    "warding": {"name": "Warding", "attribute": "minecraft:armor", "operation": "add_value", "min": 1.0, "max": 3.0, "steps": 2,
                "cost": 2, "group": "warding"},
    "might": {"name": "Might", "attribute": "minecraft:attack_damage", "operation": "add_value", "min": 0.5, "max": 1.5, "steps": 2,
              "cost": 2, "group": "might"},
}
GEMS = {
    "amethyst": {"name": "Amethyst", "item": "minecraft:amethyst_shard",
                 "stat": {"attribute": "spell_power:arcane", "operation": "add_value", "amount": 1.0}, "cost": 2},
    "emerald": {"name": "Emerald", "item": "minecraft:emerald",
                "stat": {"attribute": "minecraft:luck", "operation": "add_value", "amount": 1.0}, "cost": 1},
    "diamond": {"name": "Diamond", "item": "minecraft:diamond",
                "stat": {"attribute": "minecraft:max_health", "operation": "add_value", "amount": 2.0}, "cost": 2},
    "lapis": {"name": "Lapis", "item": "minecraft:lapis_lazuli",
              "stat": {"attribute": "minecraft:armor", "operation": "add_value", "amount": 1.0}, "cost": 1},
    "quartz": {"name": "Quartz", "item": "minecraft:quartz",
               "stat": {"attribute": "minecraft:attack_damage", "operation": "add_value", "amount": 0.5}, "cost": 1},
}
RUNES = {
    "radiance": {"name": "Rune of Radiance", "item": "minecraft:glowstone_dust",
                 "stat": {"attribute": "spell_power:arcane", "operation": "add_value", "amount": 1.0}, "cost": 2,
                 "substrates": ["gold", "netherite"]},
    "swiftness": {"name": "Rune of Swiftness", "item": "minecraft:sugar",
                  "stat": {"attribute": "minecraft:movement_speed", "operation": "add_multiplied_base", "amount": 0.03}, "cost": 2},
    "warding": {"name": "Rune of Warding", "item": "minecraft:obsidian",
                "stat": {"attribute": "minecraft:armor", "operation": "add_value", "amount": 2.0}, "cost": 3},
    "vigor": {"name": "Rune of Vigor", "item": "minecraft:golden_carrot",
              "stat": {"attribute": "minecraft:max_health", "operation": "add_value", "amount": 2.0}, "cost": 2},
}
# The most each attribute may add on one ring, whatever is rolled, inscribed and set (checked by check_mod_data).
ATTRIBUTE_CAPS = {"spell_power:arcane": 8.0, "minecraft:max_health": 14.0, "minecraft:luck": 6.0, "minecraft:movement_speed": 0.15,
                  "minecraft:armor": 10.0, "minecraft:attack_damage": 4.0}
ATTRIBUTE_NAMES = {"spell_power:arcane": "Arcane spell power", "minecraft:max_health": "Max health", "minecraft:luck": "Luck",
                   "minecraft:movement_speed": "Speed", "minecraft:armor": "Armor", "minecraft:attack_damage": "Attack damage"}

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "runesmithing": {
        "name": "Runesmithing",
        "principle": "strata",
        "tradition": "runesmiths",
        "stage": "practitioner",
        "icon": rid("resonant_ring"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{ARTIFICE_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{ARTIFICE_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{ARTIFICE_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: three different enhancements (reforge, inscribe, socket, bond, repair) performed.
            "mastered": [{"type": "practice", "activity": ARTIFICE_PRACTICE, "distinct": 3}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Raw Material", "Examine something a smith works with", "task", "minecraft:flint"),
            "observed": ("An Eye for Metal", "Examine three different things a smith works with", "task", "minecraft:gold_nugget"),
            "understood": ("Runesmithing", "Understand Runesmithing: study smithing stock at a Lampwright's Bench", "goal",
                           rid("resonant_ring")),
            "mastered": ("Master Artificer", "Enhance a ring in three different ways", "challenge", rid("artificer_bench")),
        },
        "locked": {"encountered": "Examine something a smith works with first",
                   "observed": "Examine three different things a smith works with first",
                   "understood": "Understand Runesmithing first", "mastered": "Master Runesmithing first"},
    },
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "resonant_ring": {"name": "Resonant Ring",
                      "tooltip": "Forged at an Artificer's Bench. Wear it in a ring slot; its properties are rolled once."},
}
BLOCKS = {"artificer_bench": {"name": "Artificer's Bench"}}
DEVICE_TOOLTIPS = {
    "artificer_bench": "Use with 4 ingots (1 netherite): forge a ring. Use with a ring: enhance it (the codex lists how).",
}

MESSAGES = {
    "artifice.forged": "Forged: %s",
    "artifice.done": "%s: %s",
    "artifice.refused": "Refused: %s",
    "artifice.ring": "%s %s ring; capacity %s of %s used; wear %s / %s",
    "artifice.affix": "  %s %s",
    "artifice.rune": "  rune: %s",
    "artifice.gem": "  gem: %s",
    "artifice.bond": "  bonded to %s",
    "artifice.dull": "  dull: repair it to use it again",
    "artifice.keeps": "%s keeps %s; destroys %s",
    "artifice.confirm": "Salvage gives back %s %s and %s gem(s), and destroys the rest; use again within 10 seconds to salvage",
    "artifice.salvaged": "Salvaged: %s %s and %s gem(s) back",
    "artifice.gem_back": "%s comes back",
    "artifice.repaired": "Repaired: wear %s / %s",
}
TOOLTIPS = {
    "artifice_specimen": "Smithing stock: sneak and use to examine it",
    "artifice.ring": "%s %s ring",
    "artifice.bonded": "Bonded",
    "artifice.dull": "Dull: repair it to use it again",
}


def lang_entries(lang):
    lang[f"tag.item.{ARTIFICE_SPECIMEN_TAG.replace(':', '.')}"] = "Smithing Stock"
    for key, (name, text) in PROCESSES.items():
        lang[f"compose.{MOD}.artifice.process.{key}"] = name
        lang[f"compose.{MOD}.artifice.rule.{key}"] = text
    for key, text in REFUSALS.items():
        lang[f"compose.{MOD}.artifice.refusal.{key}"] = text
    for key, (bonus, affixes, weight) in QUALITIES.items():
        lang[f"compose.{MOD}.artifice.quality.{key}"] = key.capitalize()
    for key, info in SUBSTRATES.items():
        lang[f"compose.{MOD}.artifice.substrate.{key}"] = info["name"]
    for key, info in AFFIXES.items():
        lang[f"compose.{MOD}.artifice.affix.{key}"] = info["name"]
    for key, info in GEMS.items():
        lang[f"compose.{MOD}.artifice.gem.{key}"] = info["name"]
    for key, info in RUNES.items():
        lang[f"compose.{MOD}.artifice.rune.{key}"] = info["name"]
    for key, text in ATTRIBUTE_NAMES.items():
        lang[f"compose.{MOD}.artifice.attribute.{key.replace(':', '.')}"] = text
    for part in ("substrate", "quality", "affixes", "runes", "gems", "bond", "durability"):
        lang[f"compose.{MOD}.artifice.part.{part}"] = {"durability": "wear"}.get(part, part)
    lang[f"compose.{MOD}.artifice.nothing"] = "nothing"
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text
    lang[f"jei.{MOD}.artifice.ring"] = ("Forged at an Artificer's Bench from 4 copper, iron or gold ingots or 1 netherite ingot, "
                                       "and Focus. Its quality and affixes are rolled once, on the server, when it is forged.")


# ------------------------------------------------------------------------------------------------- codex

def codex():
    substrates = "\\\n".join(f"- **{info['name']}**: {info['cost']} ingot(s), capacity {info['capacity']}, {info['sockets']} socket(s), "
                             f"{info['runes']} rune slot(s); salvage gives back {info['salvage']}" for info in SUBSTRATES.values())
    rules = "\\\n".join(f"- **{name}**: {text}" for name, text in PROCESSES.values())
    gems = "\\\n".join(f"- **{info['name']}** ({info['item'].split(':')[1].replace('_', ' ')}): capacity {info['cost']}"
                       for info in GEMS.values())
    runes = "\\\n".join(f"- **{info['name']}** ({info['item'].split(':')[1].replace('_', ' ')}): capacity {info['cost']}"
                        for info in RUNES.values())
    return {
        ("artifice", "runesmithing"): {
            "name": "Runesmithing", "x": 0, "y": 0, "icon": rid("resonant_ring"), "condition": None,
            "description": "Rings, runes and gems: research",
            "pages": [
                ("text", "Runesmithing",
                 "Once you understand First Light, look closely at smithing stock. **Sneak and use** one of these to "
                 "examine it:\\\n" + "\\\n".join(f"- {name}" for name in ARTIFICE_SPECIMENS.values())),
                ("text", "Understood", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**. "
                 "To **master** Runesmithing, enhance rings in three different ways.", ("runesmithing", "observed")),
            ],
        },
        ("artifice", "forging"): {
            "name": "Forging", "x": 2, "y": 0, "icon": rid("artificer_bench"), "condition": ("runesmithing", "understood"),
            "description": "The Artificer's Bench and substrates",
            "pages": [
                ("crafting_recipe", "Artificer's Bench",
                 f"Use it with ingots in hand to **forge** a Resonant Ring ({FORGE_FOCUS} Focus). Its quality and affixes "
                 f"are rolled once, on the server, and kept on the ring; you see them after they are made.",
                 rid("artificer_bench")),
                ("text", "Substrates", substrates),
                ("text", "Capacity",
                 f"A ring's capacity is its substrate's, plus its quality (crude -1, sound 0, fine +1, masterwork +2), "
                 f"plus {BOND_CAPACITY} while bonded. Affixes, runes and gems use it; nothing goes beyond it."),
            ],
        },
        ("artifice", "enhancing"): {
            "name": "Enhancing", "x": 4, "y": 0, "icon": rid("resonant_ring"), "condition": ("runesmithing", "understood"),
            "description": "What each process keeps and destroys",
            "pages": [
                ("text", "At the Bench",
                 "Hold the ring in your main hand and use the bench with, in your other hand: nothing (inspect it); "
                 f"redstone (**reforge**, {REFORGE_FOCUS} Focus); a rune's ingredient (**inscribe**, "
                 f"{INSCRIBE_FOCUS} Focus); a gem (**socket**); shears (**unsocket** the last gem); its substrate's "
                 f"ingot (**repair**); a lead (**bond** it to you, {BOND_FOCUS} Focus, or unbond it); flint "
                 f"(**salvage**, used twice to confirm)."),
                ("text", "What Each Keeps", rules),
                ("text", "Gems", gems),
                ("text", "Runes", runes),
                ("text", "Wearing",
                 "Wear the ring in a **ring slot**. It wears one point every 30 seconds worn; at its last point it is "
                 "**dull** and gives nothing until repaired, but it never breaks. A bonded ring serves only its bond."),
            ],
        },
    }


CATEGORY = {"artifice": {"name": "Artifice", "icon": rid("resonant_ring"), "sort": 10,
                         "description": "Rings forged, reforged, inscribed, socketed and bonded"}}


# ------------------------------------------------------------------------------------------------- data and assets

def tags(tags):
    for ref in ARTIFICE_SPECIMENS:
        tags.add("item", ARTIFICE_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("artificer_bench"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    write(assets / "models" / "block" / "artificer_bench.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": rid("block/artificer_bench_top"), "side": rid("block/artificer_bench_side"),
                     "bottom": rid("block/artificer_bench_bottom")}})
    write(assets / "blockstates" / "artificer_bench.json", {"variants": {"": {"model": rid("block/artificer_bench")}}})
    for name in ("artificer_bench", "resonant_ring"):
        write(assets / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{name}")}})
        write(assets / "models" / "item" / f"{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{name}")}})
    write(data / "loot_table" / "blocks" / "artificer_bench.json", self_drop("artificer_bench"))
    write(data / "recipe" / "artificer_bench.json", dict({"fabric:load_conditions": condition("concordance")}, **{
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IAI", "SBS", "S S"],
        "key": {"I": "minecraft:iron_ingot", "A": "minecraft:amethyst_shard", "S": "minecraft:smooth_stone",
                "B": "minecraft:smithing_table"},
        "result": {"id": rid("artificer_bench"), "count": 1}}))
    # Trinkets Updated: rings go in the hand ring slot, which players get.
    write(data.parent / "trinkets" / "tags" / "item" / "hand" / "ring.json", {"replace": False, "values": [rid("resonant_ring")]})
    write(data.parent / "trinkets" / "entities" / f"{MOD}.json", {"entities": ["player"], "slots": ["hand/ring"]})


def write_data(write, data):
    for key, info in SUBSTRATES.items():
        body = {k: v for k, v in info.items() if k != "name"}
        body["affixes"] = [rid(a) for a in info["affixes"]]
        write(data / "concordance" / "substrate" / f"{key}.json", dict({"schema": 1}, **body))
    for key, info in AFFIXES.items():
        write(data / "concordance" / "affix" / f"{key}.json", dict({"schema": 1}, **{k: v for k, v in info.items() if k != "name"}))
    for key, info in GEMS.items():
        write(data / "concordance" / "gem" / f"{key}.json", dict({"schema": 1}, **{k: v for k, v in info.items() if k != "name"}))
    for key, info in RUNES.items():
        body = {k: v for k, v in info.items() if k != "name"}
        if "substrates" in body:
            body["substrates"] = [rid(s) for s in body["substrates"]]
        write(data / "concordance" / "rune" / f"{key}.json", dict({"schema": 1}, **body))
