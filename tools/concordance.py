"""The Arcane Concordance: Jugcraft's magic system (docs/ARCANE_CONCORDANCE.md). This module is the canonical source for
its vocabulary, numbers and first content (docs/features/arcane-concordance-first-light.md, milestone 1, "First Light").

Everything the game and the codex say about the Concordance is generated from the tables here, so a number cannot
differ between the research rules, the spell, the bench, the codex pages, the recipe viewer and the tests:

- data/jugcraft/concordance/{research,invocation,working,conversion,component,instrument}/*.json: the server-owned
  rules, read by concordance/ConcordanceData.java (a data pack can override them; the server validates them on every
  reload); components and instruments are the composition grammar (roadmap step 8, concordance/compose);
- data/jugcraft/spell/*.json: the Spell Engine spells the invocations cast, and the carrier for inscribed spells
  (tools/check_mod_data.py lints them);
- data/jugcraft/modonomicon/...: the codex (a Modonomicon book) and the research bridge that shows its pages
  (Jugcraft awards an advancement on the server; Modonomicon turns it into a research fact; the fact unlocks pages);
- data/jugcraft/spell_assignments/*.json: Spell Engine opt-outs for Jugcraft arms that have their own weapon arts;
- assets/jugcraft/player_animations, dynamiclights, lang, models, items: presentation;
- assets/jugcraft/recipe_view.json "concordance": the bench's workings for JEI.

Java mirrors a few constants (Focus, motes, the lantern); tools/check_mod_data.py check_concordance() keeps them equal.

The ten Principles, seven resources, sixteen traditions and five stages below are NEW PROPOSALS made for this system on
5 October 2026. Nothing earlier in Jugcraft defined them; they extend (and do not rename) the eight school seeds in
docs/CONTENT_BRANCHES.md. Their ids become stable once released.
"""
MOD = "jugcraft"
FEATURE = "concordance"
BOOK = "arcane_concordance"

# ----------------------------------------------------------------------------------------------- vocabulary (proposed)

# The ten Principles: the aspects of the world a practice works with. A Principle is knowledge and affinity, never a
# currency: it names essences, research and invocations, and maps to a Spell Power school only where combat scaling fits.
PRINCIPLES = {
    "radiance": {"name": "Radiance", "domain": "light, revelation and sight", "school": "spell_power:arcane",
                 "seed": None, "color": 0xF4D27A},
    "ember": {"name": "Ember", "domain": "heat, combustion and change by fire", "school": "spell_power:fire",
              "seed": "Fire", "color": 0xF0743C},
    "rime": {"name": "Rime", "domain": "cold, stillness and preservation", "school": "spell_power:frost",
             "seed": "Ice", "color": 0xA9E4F2},
    "tempest": {"name": "Tempest", "domain": "storm, charge and sudden motion", "school": "spell_power:lightning",
                "seed": "Storm", "color": 0xF2EE8A},
    "strata": {"name": "Strata", "domain": "stone, structure and weight", "school": None, "seed": "Earth",
               "color": 0xA88A62},
    "verdance": {"name": "Verdance", "domain": "growth, life and renewal", "school": "spell_power:healing",
                 "seed": None, "color": 0x7CC46A},
    "tide": {"name": "Tide", "domain": "flow, solution and exchange", "school": None, "seed": None, "color": 0x4C8FD6},
    "tether": {"name": "Tether", "domain": "binding, sympathy and agreements", "school": None,
               "seed": "Cursing", "color": 0xB48AD8},
    "echo": {"name": "Echo", "domain": "memory, sound, dreams and knowledge", "school": "spell_power:arcane",
             "seed": None, "color": 0xC9B8E8},
    "hollow": {"name": "Hollow", "domain": "absence, decay and endings", "school": "spell_power:soul",
               "seed": "Necromancy, Blood", "color": 0x5E5A7A},
}

# The seven resource categories. None converts into another except through an explicit conversion recipe.
RESOURCES = {
    "focus": {"name": "Focus", "kind": "personal attention", "unit": "point",
              "summary": "A practitioner's own capacity to cast. It returns with time, lives with the player, and "
                         "can never be stored in an item, traded or carried by a device."},
    "ley_charge": {"name": "Ley Charge", "kind": "operational energy", "unit": "ley",
                   "summary": "Stored magical working energy in devices and conduits. It powers stations and "
                              "rituals; it is not light, heat or life."},
    "essence": {"name": "Principle Essence", "kind": "material value", "unit": "measure",
                "summary": "A measured quantity of one Principle drawn from a specimen. Essences of different "
                           "Principles never substitute for one another."},
    "vitae": {"name": "Vitae", "kind": "biological expenditure", "unit": "drop",
              "summary": "Life given deliberately in an offering. It is separate from health and from the "
                         "exhaustion that offering leaves behind."},
    "astral_resonance": {"name": "Astral Resonance", "kind": "celestial alignment", "unit": "resonance",
                         "summary": "Alignment gathered when the sky is right. It is recorded per event so the same "
                                    "alignment never pays twice."},
    "bound_will": {"name": "Bound Will", "kind": "supernatural identity", "unit": "record",
                   "summary": "A record of an agreement with a named spirit or construct: an obligation with an "
                              "identity, never an anonymous fluid."},
    "prima_materia": {"name": "Prima Materia", "kind": "equivalence value", "unit": "grain",
                      "summary": "The exact value assigned to an eligible mundane material for transmutation, rounded "
                                 "down and never created from nothing."},
}

# The five stages of the Concordance, against Jugcraft's own stages (docs/DESIGN.md).
STAGES = {
    "initiate": {"name": "Initiate", "jugcraft": "Discovery"},
    "practitioner": {"name": "Practitioner", "jugcraft": "Workshops"},
    "adept": {"name": "Adept", "jugcraft": "Specialization"},
    "master": {"name": "Master", "jugcraft": "Expeditions"},
    "architect": {"name": "Architect", "jugcraft": "Shared wonders"},
}

# The four states of a research entry for one player.
RESEARCH_STAGES = ["encountered", "observed", "understood", "mastered"]

# The sixteen traditions: specialties a player can follow without mastering the rest.
TRADITIONS = {
    "lampwrights": {"name": "Lampwrights", "principles": ["radiance"], "roadmap": 5,
                    "practice": "illumination, lanterns and revelation"},
    "hearthbinders": {"name": "Hearthbinders", "principles": ["ember"], "roadmap": 10,
                      "practice": "controlled heat for kilns, forges and cooking"},
    "rimekeepers": {"name": "Rimekeepers", "principles": ["rime"], "roadmap": 10,
                    "practice": "preservation, cooling and barriers"},
    "stormcallers": {"name": "Stormcallers", "principles": ["tempest"], "roadmap": 10,
                     "practice": "charging, weather instruments and swift movement"},
    "stratawrights": {"name": "Stratawrights", "principles": ["strata"], "roadmap": 10,
                      "practice": "soil care, shaping and mineral sensing"},
    "greenwardens": {"name": "Greenwardens", "principles": ["verdance"], "roadmap": 14,
                     "practice": "habitats, cultivation and living devices"},
    "alembists": {"name": "Alembists", "principles": ["tide", "ember", "verdance"], "roadmap": 13,
                  "practice": "experimental alchemy"},
    "circlewrights": {"name": "Circlewrights", "principles": ["tether", "strata"], "roadmap": 12,
                      "practice": "ritual structures, anchors and containment"},
    "spiritbinders": {"name": "Spiritbinders", "principles": ["tether", "hollow"], "roadmap": 17,
                      "practice": "agreements with spirits and familiars"},
    "clockhearts": {"name": "Clockhearts", "principles": ["strata", "tempest"], "roadmap": 17,
                    "practice": "constructs and accountable logistics"},
    "crimson_vigil": {"name": "Crimson Vigil", "principles": ["verdance", "hollow"], "roadmap": 16,
                      "practice": "offerings of Vitae and living equipment"},
    "starwatchers": {"name": "Starwatchers", "principles": ["radiance", "echo"], "roadmap": 15,
                     "practice": "celestial cycles and attunement"},
    "runesmiths": {"name": "Runesmiths", "principles": ["strata", "echo"], "roadmap": 19,
                   "practice": "equipment, runes, sockets and relics"},
    "hexweavers": {"name": "Hexweavers", "principles": ["tether", "hollow"], "roadmap": 22,
                   "practice": "sympathetic links, curses and wards"},
    "dreamwalkers": {"name": "Dreamwalkers", "principles": ["echo", "tide"], "roadmap": 22,
                     "practice": "dream expeditions and memory"},
    "balancewrights": {"name": "Balancewrights", "principles": ["strata", "tide"], "roadmap": 21,
                       "practice": "bounded material equivalence"},
}

# --------------------------------------------------------------------------------------------- milestone 1 numbers

# Focus (Java: concordance/FocusPool.java). One point returns every FOCUS_REGEN_TICKS game ticks, worked out from the
# game time when it is read, so nothing ticks per player.
FOCUS_MAX = 20
FOCUS_REGEN_TICKS = 40

# Examining a specimen counts as "in darkness" at or below this light (Java: Examination.DARK_LIGHT). Night in the open
# reads 4, so an Initiate can study by a dark cave or under the night sky.
DARK_LIGHT = 4
# Distinct specimens examined in darkness that understand Radiance without a bench (the field route).
FIELD_SPECIMENS = 3
# Distinct chunks a Kindle must light to master First Light.
MASTERY_CHUNKS = 8

# Kindle (the illumination invocation; Java: KindleInvocation.java and LumenMoteBlock.java).
KINDLE_FOCUS = 4
KINDLE_MASTERED_FOCUS = 3
KINDLE_RANGE = 16
KINDLE_COOLDOWN_SECONDS = 1.5
KINDLE_CAST_SECONDS = 0.6
MOTE_LIGHT = 14
MOTE_STEP_TICKS = 80
KINDLE_MOTE_STEPS = 15  # a Kindled mote lasts 15 steps of 80 ticks: 60 seconds
TRAIL_LIGHT = 12
TRAIL_CHECK_TICKS = 10

# The Kindled Lantern (Java: KindledLanternItem.java). Radiance is measured in "measures" of Radiance essence.
LANTERN_CAPACITY = 64
LANTERN_BURN_TICKS = 400  # a lit lantern burns one measure every 20 seconds, carried or not
LANTERN_START = 8  # the measures the amethyst shard leaves in a newly kindled lantern

# The Lampwright's Bench (Java: LampwrightBenchBlockEntity.java).
STUDY_TICKS = 100
CHANNEL_FOCUS = 6
CHANNEL_RADIANCE = 2

# Research Notes (Java: ResearchNotesItem.java, ResearchNotes.java).
NOTES_COOLDOWN_TICKS = 10
NOTES_MAX_ENTRIES = 32

# The Lumen Sconce (Java: LumenSconceBlockEntity.java): holds a lantern's worth of Radiance and burns it for light 15.
SCONCE_BURN_TICKS = 1200  # one measure a minute: a full sconce burns for 64 minutes
SCONCE_POUR = 16  # the most one pour or draw moves
SCONCE_RATE_LIMIT = 32  # the most that moves through one sconce per window
SCONCE_RATE_WINDOW = 20

# Luminous specimens: what can be examined and studied, and the Radiance each yields when infused into a lantern.
# Amethyst is the first: geodes are common, early and already Jugcraft's magic crystal.
SPECIMENS = {
    "minecraft:amethyst_shard": 8,
    "minecraft:glowstone_dust": 6,
    f"{MOD}:glowcap": 4,
    f"{MOD}:glimmerbloom": 3,
    f"{MOD}:jack_o_lantern_mushroom": 3,
    "minecraft:glow_ink_sac": 3,
    "minecraft:glow_lichen": 2,
    "minecraft:glow_berries": 1,
}
SPECIMEN_TAG = f"{MOD}:luminous_specimens"
INSTRUMENT_TAG = f"{MOD}:concordance_instruments"
# What the first Concordance pieces (the bench, the wand, the sconce, the codex) take for their light: an amethyst shard,
# or glowstone dust where amethyst cannot be had (roadmap step 24's recovery route: amethyst grows only in geodes).
LUMINOUS_MATTER_TAG = f"{MOD}:concordance/luminous_matter"
LUMINOUS_MATTER = ["minecraft:amethyst_shard", "minecraft:glowstone_dust"]
SPELL_TAG = f"{MOD}:concordance"

# --------------------------------------------------------------------------------------------- milestone 1 content

ITEMS = {
    "initiate_wand": {"name": "Initiate's Wand",
                      "tooltip": "A first instrument. Hold it to cast the invocations you have understood; a "
                                 "Concordance invocation needs a Concordance instrument in the main hand."},
    "research_notes": {"name": "Research Notes",
                       "tooltip": "Use blank to write down what you know; others read what you wrote"},
    "kindled_lantern": {"name": "Kindled Lantern",
                        "tooltip": "Holds Radiance. Use it to light or put it out; lit and in either hand it lights "
                                   "the way round you, and it burns its Radiance whether carried or not. Recharge it "
                                   "at a Lampwright's Bench."},
}
BLOCKS = {
    "lampwright_bench": {"name": "Lampwright's Bench"},
    "lumen_sconce": {"name": "Lumen Sconce"},
}
# Items that cast Concordance invocations from the main hand (#jugcraft:concordance_instruments).
INSTRUMENTS = ["initiate_wand"]
# Blocks with no item: the Kindled mote is light, not a thing to hold.
ITEMLESS_BLOCKS = {"lumen_mote": {"name": "Kindled Light"}}

RESEARCH = {
    "first_light": {
        "name": "First Light",
        "principle": "radiance",
        "tradition": "lampwrights",
        "stage": "initiate",
        "icon": "minecraft:amethyst_shard",
        "requires": [],
        # Each state needs the one before it and one of its alternatives.
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{SPECIMEN_TAG}", "max_light": DARK_LIGHT}],
            "understood": [
                {"type": "study", "station": f"{MOD}:lampwright_bench", "specimens": f"#{SPECIMEN_TAG}"},
                {"type": "examine", "specimens": f"#{SPECIMEN_TAG}", "max_light": DARK_LIGHT,
                 "distinct": FIELD_SPECIMENS},
                # Shared records: another player's notes from someone who understood it. The reader must still
                # encounter and observe it themselves, and notes never stand for mastery.
                {"type": "notes"},
            ],
            "mastered": [{"type": "invoke", "invocation": f"{MOD}:kindle", "distinct_chunks": MASTERY_CHUNKS}],
        },
        "unlocks": {"understood": {"invocations": [f"{MOD}:kindle", f"{MOD}:aegis", f"{MOD}:revelation"],
                                   "workings": [f"{MOD}:kindle_lantern", f"{MOD}:infuse_lantern",
                                                f"{MOD}:channel_lantern"]},
                    "mastered": {"invocations": [f"{MOD}:lance", f"{MOD}:flashstep", f"{MOD}:lanternward"]}},
    },
}

# Authored invocations (roadmap step 10; Java: Definitions.Invocation, ConcordanceRules.compile, Invocations.java).
# Each is a composition in the shared grammar, compiled by the same compiler under the same instrument limits as a
# player's; its authored words (COMPONENTS with "authored") cannot be composed by players. "work" and "persists" are the
# most work one cast spends and the most ticks anything it makes lasts, over every tuning (checked by the loader and
# by tools/check_mod_data.py). "tunings" are the modifiers a player may join to it, one at a time. "cast" and
# "cooldown" are seconds (Spell Engine's; Jugcraft raises the cooldown to the composition's own if that is longer).
# "counter" is how an opponent or the world answers it, for the codex.
INVOCATION_MAX_TUNINGS = 3  # Java: RulesParser.MAX_TUNINGS
INVOCATION_SCALING_MAX = 2.0  # Java: RulesParser.MAX_SCALING (damage per point of Spell Power)
# The vanilla particles invocations show on release (Spell Engine skips an unknown id, so a typo would show nothing).
INVOCATION_PARTICLES = ("minecraft:end_rod", "minecraft:wax_on", "minecraft:glow", "minecraft:electric_spark",
                        "minecraft:cloud", "minecraft:flame", "minecraft:small_flame", "minecraft:lava")
INVOCATION_ROLES = {
    "damage": "Damage", "defense": "Defense", "movement": "Movement", "support": "Support",
    "investigation": "Investigation", "utility": "Utility",
}
INVOCATIONS = {
    "kindle": {
        "name": "Kindle", "role": "utility", "composition": "ray struck kindle_light", "stage": "understood",
        "focus": KINDLE_FOCUS, "mastered_focus": KINDLE_MASTERED_FOCUS, "tunings": ["extend"], "work": 3,
        "persists": KINDLE_MOTE_STEPS * MOTE_STEP_TICKS, "cast": KINDLE_CAST_SECONDS, "cooldown": KINDLE_COOLDOWN_SECONDS,
        "animation": ("kindle_cast", "kindle_release"), "sounds": ("concordance.kindle_gather", "concordance.kindle"),
        "particle": "minecraft:end_rod", "icon": "minecraft:glowstone_dust",
        "description": f"Sets a mote of steady light in the open block where you look, up to {KINDLE_RANGE} blocks "
                       f"away, for {KINDLE_MOTE_STEPS * MOTE_STEP_TICKS // 20} seconds.",
        "counter": "The mote lights only open air you may build in, goes out on its own, and a block placed in its "
                   "space replaces it.",
        "fizzle": f"Nothing there to light: Kindle needs open air within %s blocks",
    },
    "aegis": {
        "name": "Dawn Aegis", "role": "defense", "composition": "here struck dawn_aegis", "stage": "understood",
        "focus": 5, "mastered_focus": 4, "tunings": ["prolong", "intensify"], "work": 1, "persists": 400,
        "cast": 0.4, "cooldown": 12.0,
        "animation": ("aegis_cast", "aegis_release"), "sounds": ("concordance.kindle_gather", "concordance.aegis"),
        "particle": "minecraft:wax_on", "icon": "minecraft:shield",
        "description": "Wraps you in a shell of dawn light that absorbs the next blows.",
        "counter": "A burst of damage breaks the shell, it fades after 10 seconds, and the cooldown leaves two "
                   "seconds bare; it does not stack with itself.",
        "fizzle": "The light finds nothing to shield",
    },
    "revelation": {
        "name": "Revelation", "role": "investigation", "composition": "here survey reveal", "stage": "understood",
        "focus": 3, "mastered_focus": 2, "tunings": ["prolong"], "work": 6, "persists": 400,
        "cast": 0.8, "cooldown": 8.0,
        "animation": ("reveal_cast", "reveal_release"), "sounds": ("concordance.kindle_gather", "concordance.revelation"),
        "particle": "minecraft:glow", "icon": "minecraft:spyglass",
        "description": "Makes the creatures round you glow, seen through walls.",
        "counter": "Milk clears the glow, it reaches only 6 blocks, and it never reveals your party or players you "
                   "may not harm.",
        "fizzle": "Nothing hides nearby",
    },
    "lance": {
        "name": "Lance of Dawn", "role": "damage", "composition": "ray struck lance_beam", "stage": "mastered",
        "focus": 5, "mastered_focus": 4, "tunings": ["extend", "intensify"], "work": 2, "persists": 0,
        "cast": 0.5, "cooldown": 2.0,
        "animation": ("lance_cast", "lance_release"), "sounds": ("concordance.kindle_gather", "concordance.lance"),
        "particle": "minecraft:electric_spark", "icon": "minecraft:spectral_arrow",
        "description": "A narrow beam of light: arcane damage to the first creature it meets, stronger with arcane "
                       "Spell Power.",
        "counter": "It needs a clear line, strikes one target and telegraphs a half-second cast; magic resistance "
                   "reduces it, though armour and shields do not.",
        "fizzle": "The beam meets nothing to strike",
    },
    "flashstep": {
        "name": "Flashstep", "role": "movement", "composition": "here struck flash", "stage": "mastered",
        "focus": 4, "mastered_focus": 3, "tunings": ["intensify"], "work": 1, "persists": 0,
        "cast": 0.0, "cooldown": 4.0,
        "animation": (None, "flash_release"), "sounds": (None, "concordance.flash"),
        "particle": "minecraft:cloud", "icon": "minecraft:feather",
        "description": "Carries you a few blocks the way you face, in a flash.",
        "counter": "Walls stop it, knockback resistance shortens it, and it cannot be used again for 4 seconds.",
        "fizzle": "Something holds you in place",
    },
    "lanternward": {
        "name": "Lanternward", "role": "support", "composition": "here allies mend ward", "stage": "mastered",
        "focus": 7, "mastered_focus": 6, "tunings": ["widen", "prolong"], "work": 8, "persists": 400,
        "cast": 1.0, "cooldown": 15.0,
        "animation": ("ward_cast", "ward_release"), "sounds": ("concordance.kindle_gather", "concordance.lanternward"),
        "particle": "minecraft:end_rod", "icon": f"{MOD}:kindled_lantern",
        "description": "Mends you and the members of your party round you, and wards them with light.",
        "counter": "Allies must stand within 4 blocks, only your party is reached, and the one-second cast can be "
                   "interrupted.",
        "fizzle": "No one nearby needs the light",
    },
}
for _key, _invocation in INVOCATIONS.items():
    _invocation.update({"principle": "radiance", "research": f"{MOD}:first_light"})

WORKINGS = {
    "kindle_lantern": {"type": "craft", "work": "minecraft:lantern", "specimen": "minecraft:amethyst_shard",
                       "result": f"{MOD}:kindled_lantern", "radiance": LANTERN_START},
    "infuse_lantern": {"type": "infuse", "work": f"{MOD}:kindled_lantern", "specimens": SPECIMENS},
    "channel_lantern": {"type": "channel", "work": f"{MOD}:kindled_lantern", "conversion": f"{MOD}:focus_to_radiance"},
}

# Explicit conversions between resource types (Java: resource/Conversion, ConversionTable). Nothing converts without
# one, and the loader refuses any set that would let a loop of conversions lose nothing.
CONVERSIONS = {
    "focus_to_radiance": {"from": {"resource": "focus", "amount": CHANNEL_FOCUS},
                          "to": {"resource": "essence/radiance", "amount": CHANNEL_RADIANCE}},
}
# ------------------------------------------------------------------------- composition and effects (roadmap 8 and 9)

# The ten common effect operations (Java: concordance/effect/EffectKind.java): what each acts on and the work units
# one application costs, the unit composed spells are capped in.
EFFECT_KINDS = {
    "damage": {"on": "creature", "work": 1, "name": "Damage"},
    "restoration": {"on": "creature", "work": 1, "name": "Restoration"},
    "movement": {"on": "creature", "work": 1, "name": "Movement"},
    "illumination": {"on": "block", "work": 2, "name": "Illumination"},
    "status": {"on": "creature", "work": 1, "name": "Status"},
    "interaction": {"on": "block", "work": 2, "name": "Interaction"},
    "harvesting": {"on": "block", "work": 3, "name": "Harvesting"},
    "protection": {"on": "creature", "work": 1, "name": "Protection"},
    "detection": {"on": "creature", "work": 1, "name": "Detection"},
    "alteration": {"on": "block", "work": 3, "name": "Alteration"},
}
# Creatures that harmful control (pushes and statuses) does not move or slow, and those it reaches at half strength.
EFFECT_IMMUNE = ["minecraft:ender_dragon", "minecraft:wither", "minecraft:warden", "minecraft:elder_guardian"]
EFFECT_RESISTANT = ["minecraft:iron_golem", "minecraft:ravager", "minecraft:piglin_brute"]
# Blocks an interaction effect may use as a player would, and plants a harvesting effect may gather (besides ripe
# crops, which are always gathered and replanted).
# Vanilla tags are optional entries, so a renamed tag in a later version empties the entry instead of breaking the list.
INTERACTABLE = ["minecraft:lever", {"id": "#minecraft:buttons", "required": False},
                {"id": "#minecraft:wooden_doors", "required": False}, {"id": "#minecraft:wooden_trapdoors", "required": False},
                {"id": "#minecraft:fence_gates", "required": False}]
HARVESTABLE = [{"id": "#minecraft:small_flowers", "required": False}, "minecraft:short_grass", "minecraft:fern",
               "minecraft:pumpkin", "minecraft:melon"]
# Absolute limits of the grammar (Java: concordance/compose/Grammar.java): data can only set lower ones.
COMPOSE_LIMITS = {"MAX_TEXT": 256, "MAX_NAMES": 24, "MAX_OPERATIONS": 3, "MAX_MODIFIERS": 2, "MAX_DEPTH": 2,
                  "MAX_BRANCHES": 2, "MAX_RANGE": 32, "MAX_RADIUS": 6, "MAX_TARGETS": 16, "MAX_WORK": 256,
                  "MAX_CAPACITY": 64, "MAX_PULSES": 5, "MIN_INTERVAL": 10, "MAX_INTERVAL": 200,
                  "MAX_COMPONENT_COST": 16, "MAX_MODIFIER_AMOUNT": 200, "MIN_COOLDOWN": 10, "MAX_COOLDOWN": 200}
EFFECT_LIMITS = {"MAX_MAGNITUDE": 40, "MAX_DURATION": 2400, "MAX_AMPLIFIER": 4, "MAX_PUSH": 20}

# What each composing instrument can hold (data/jugcraft/concordance/instrument).
INSTRUMENT_LIMITS = {
    "initiate_wand": {"item": f"{MOD}:initiate_wand", "capacity": 8, "targets": 6, "work": 48, "branches": 1,
                      "duration": 1200},
}

# Roadmap step 12 (tools/concordance_rituals.py): Circle Lore, its rituals, structures and blocks join the tables above.
import concordance_rituals as rituals  # noqa: E402
RESEARCH.update(rituals.RESEARCH)
ITEMS.update(rituals.ITEMS)
BLOCKS.update(rituals.BLOCKS)
INSTRUMENTS += rituals.INSTRUMENTS
INSTRUMENT_LIMITS.update(rituals.INSTRUMENT_LIMITS)
CONVERSIONS.update(rituals.CONVERSIONS)
# Roadmap step 13 (tools/concordance_alchemy.py): the Alembic Arts, its crucible, tools and ingredients.
import concordance_alchemy as alchemy  # noqa: E402
RESEARCH.update(alchemy.RESEARCH)
ITEMS.update(alchemy.ITEMS)
BLOCKS.update(alchemy.BLOCKS)
# Roadmap step 14 (tools/concordance_ecology.py): Verdant Husbandry, its beds, crops and living devices.
import concordance_ecology as ecology  # noqa: E402
RESEARCH.update(ecology.RESEARCH)
ITEMS.update(ecology.ITEMS)
BLOCKS.update(ecology.BLOCKS)
ITEMLESS_BLOCKS.update(ecology.ITEMLESS_BLOCKS)
CONVERSIONS.update(ecology.CONVERSIONS)
# Roadmap step 15 (tools/concordance_celestial.py): Celestial Attunement, its patterns, observatory and astrolabe.
import concordance_celestial as celestial  # noqa: E402
RESEARCH.update(celestial.RESEARCH)
ITEMS.update(celestial.ITEMS)
BLOCKS.update(celestial.BLOCKS)
# Roadmap step 16 (tools/concordance_crimson.py): Crimson Rites, offerings, Vitae and the Thornheart Blade.
import concordance_crimson as crimson  # noqa: E402
RESEARCH.update(crimson.RESEARCH)
ITEMS.update(crimson.ITEMS)
# Roadmap step 17 (tools/concordance_workers.py): the Binding Arts, familiars, spirits and constructs.
import concordance_workers as workers  # noqa: E402
RESEARCH.update(workers.RESEARCH)
ITEMS.update(workers.ITEMS)
BLOCKS.update(workers.BLOCKS)
# Roadmap step 18 (tools/concordance_logistics.py): Courier Posts, requests, reservations and accountable transit.
import concordance_logistics as logistics  # noqa: E402
BLOCKS.update(logistics.BLOCKS)
# Roadmap step 19 (tools/concordance_artifice.py): Runesmithing, the Artificer's Bench and Resonant Rings.
import concordance_artifice as artifice  # noqa: E402
RESEARCH.update(artifice.RESEARCH)
ITEMS.update(artifice.ITEMS)
BLOCKS.update(artifice.BLOCKS)
# Roadmap step 20 (tools/concordance_relics.py): Relic Lore, relics in explicit contexts and the Reliquary Shrine.
import concordance_relics as relics  # noqa: E402
RESEARCH.update(relics.RESEARCH)
ITEMS.update(relics.ITEMS)
BLOCKS.update(relics.BLOCKS)
# Roadmap step 21 (tools/concordance_equivalence.py): Assay, the Assayer's Scale and the bounded catalogue.
import concordance_equivalence as equivalence  # noqa: E402
RESEARCH.update(equivalence.RESEARCH)
ITEMS.update(equivalence.ITEMS)
BLOCKS.update(equivalence.BLOCKS)
# Roadmap step 22 (tools/concordance_hexes.py): Sympathy and Dreamwalking, links, curses, wards and dreams.
import concordance_hexes as hexes  # noqa: E402
RESEARCH.update(hexes.RESEARCH)
ITEMS.update(hexes.ITEMS)
BLOCKS.update(hexes.BLOCKS)
# Roadmap step 23 (tools/concordance_conclave.py): the Starbound Conclave, its renown, commissions and projects.
import concordance_conclave as conclave  # noqa: E402
RESEARCH.update(conclave.RESEARCH)
ITEMS.update(conclave.ITEMS)
BLOCKS.update(conclave.BLOCKS)
# Roadmap step 24 (tools/concordance_progression.py): the five stages and the canonical progression graph.
import concordance_progression as progression  # noqa: E402
# Roadmap step 25 (tools/concordance_spire.py): the Concord Spire; its structures and its rite join the rituals' own.
import concordance_spire as spire  # noqa: E402
# Roadmap step 26 (tools/concordance_journal.py): the Concordance Journal's words and codex page.
import concordance_journal as journal  # noqa: E402
# Roadmap step 27 (tools/concordance_signs.py): signs of what really happened, the warnings' sounds and the codex page.
import concordance_signs as signs  # noqa: E402
ITEMS.update(spire.ITEMS)
BLOCKS.update(spire.BLOCKS)
rituals.STRUCTURES.update(spire.STRUCTURES)
rituals.RITUALS.update(spire.RITUALS)
rituals.RESEARCH["circle_lore"]["unlocks"]["understood"]["rituals"].append(spire.rid("spire_kindling"))
# Ember (tools/concordance_ember.py): Hearthbinding, the Hearthbinders' research, and their invocations (their
# authored words join COMPONENTS below).
import concordance_ember as ember  # noqa: E402
RESEARCH.update(ember.RESEARCH)
ITEMS.update(ember.ITEMS)
BLOCKS.update(ember.BLOCKS)
INVOCATIONS.update(ember.INVOCATIONS)
# Wayfaring (tools/concordance_trinkets.py): the owner's belt, boots and charms, worn once Relic Lore is understood.
import concordance_trinkets as trinkets  # noqa: E402
ITEMS.update(trinkets.ITEMS)
BLOCKS.update(trinkets.BLOCKS)
# Everything a player can examine or study: each research entry's specimens (Java: JugcraftConcordance.SPECIMENS).
SPECIMEN_TAGS = [SPECIMEN_TAG, rituals.CIRCLE_SPECIMEN_TAG, alchemy.ALCHEMY_SPECIMEN_TAG, ecology.GARDEN_SPECIMEN_TAG,
                 celestial.CELESTIAL_SPECIMEN_TAG, crimson.CRIMSON_SPECIMEN_TAG, workers.BINDING_SPECIMEN_TAG,
                 artifice.ARTIFICE_SPECIMEN_TAG, relics.RELIC_SPECIMEN_TAG, equivalence.ASSAY_SPECIMEN_TAG,
                 hexes.SYMPATHY_SPECIMEN_TAG, hexes.DREAM_SPECIMEN_TAG, ember.EMBER_SPECIMEN_TAG]
ALL_SPECIMENS_TAG = f"{MOD}:concordance_specimens"

_UNDERSTOOD = {"research": f"{MOD}:first_light", "state": "understood"}
_MASTERED = {"research": f"{MOD}:first_light", "state": "mastered"}

# The first components: the Radiance grammar, learnt from First Light. "name" and "text" are for the codex and the
# lang file; the rest is written to data/jugcraft/concordance/component/<key>.json. Operations of the other effect
# kinds arrive with the research of their Principles; the effect system already carries them (step 9).
COMPONENTS = {
    "here": {"slot": "delivery", "requires": _UNDERSTOOD, "capacity": 1, "focus": 0,
             "delivery": {"form": "here", "range": 0}, "name": "Here",
             "text": "The spell lands where you stand; after then, where the spell before it landed."},
    "touch": {"slot": "delivery", "requires": _UNDERSTOOD, "capacity": 1, "focus": 0,
              "delivery": {"form": "touch", "range": 4}, "name": "Touch",
              "text": "The spell lands on the first creature or surface within reach."},
    "ray": {"slot": "delivery", "requires": _MASTERED, "capacity": 2, "focus": 1,
            "delivery": {"form": "ray", "range": 16}, "name": "Ray",
            "text": "A straight line of light: the spell lands on the first creature or surface it meets."},
    "struck": {"slot": "selection", "requires": _UNDERSTOOD, "capacity": 0, "focus": 0,
               "selection": {"pick": "struck", "radius": 0, "targets": 1}, "name": "Struck",
               "text": "Acts on the one thing the delivery reached: the creature, or else the block."},
    "creatures": {"slot": "selection", "requires": _MASTERED, "capacity": 2, "focus": 2,
                  "selection": {"pick": "creatures", "radius": 3, "targets": 4}, "name": "Creatures",
                  "text": "Acts on the creatures nearest to where the spell lands. A harmful operation never "
                          "touches you."},
    "spread": {"slot": "selection", "requires": _MASTERED, "capacity": 2, "focus": 2,
               "selection": {"pick": "blocks", "radius": 3, "targets": 4}, "name": "Spread",
               "text": "Acts on the blocks nearest to where the spell lands that the operation can work on "
                       "(lights keep two blocks apart)."},
    "light": {"slot": "operation", "requires": _UNDERSTOOD, "capacity": 1, "focus": 2,
              "operation": {"effect": "illumination", "intent": "helpful", "principle": "radiance", "duration": 640},
              "name": "Light", "text": "Sets a Kindled mote in open air you may build in."},
    "reveal": {"slot": "operation", "requires": _UNDERSTOOD, "capacity": 1, "focus": 1,
               "operation": {"effect": "detection", "intent": "harmful", "principle": "radiance", "duration": 200},
               "name": "Reveal", "text": "Makes a creature glow, seen through walls. Revealing another player "
                                         "counts as harming them."},
    "ward": {"slot": "operation", "requires": _UNDERSTOOD, "capacity": 2, "focus": 3,
             "operation": {"effect": "protection", "intent": "helpful", "principle": "radiance", "magnitude": 4,
                           "duration": 200},
             "name": "Ward", "text": "A shell of light that absorbs harm (Absorption)."},
    "sear": {"slot": "operation", "requires": _MASTERED, "capacity": 2, "focus": 3,
             "operation": {"effect": "damage", "intent": "harmful", "principle": "radiance", "magnitude": 4,
                           "school": "spell_power:arcane"},
             "name": "Sear", "text": "Light that burns: arcane damage, credited to you."},
    "dazzle": {"slot": "operation", "requires": _MASTERED, "capacity": 1, "focus": 2,
               "operation": {"effect": "status", "intent": "harmful", "principle": "radiance", "magnitude": 0,
                             "duration": 60, "status": "minecraft:slowness"},
               "name": "Dazzle", "text": "Dazzled creatures stumble (Slowness)."},
    "intensify": {"slot": "modifier", "requires": _MASTERED, "capacity": 1, "focus": 2,
                  "modifier": {"aspect": "magnitude", "amount": 50}, "name": "Intensify",
                  "text": "Half as strong again (at least one more): joined to an operation."},
    "prolong": {"slot": "modifier", "requires": _UNDERSTOOD, "capacity": 1, "focus": 1,
                "modifier": {"aspect": "duration", "amount": 100}, "name": "Prolong",
                "text": "Twice as long: joined to an operation that lasts."},
    "widen": {"slot": "modifier", "requires": _MASTERED, "capacity": 1, "focus": 2,
              "modifier": {"aspect": "radius", "amount": 2}, "name": "Widen",
              "text": "Two blocks wider: joined to creatures or spread."},
    "extend": {"slot": "modifier", "requires": _MASTERED, "capacity": 1, "focus": 1,
               "modifier": {"aspect": "range", "amount": 8}, "name": "Extend",
               "text": "Eight blocks further: joined to touch or ray."},
    "pulse": {"slot": "termination", "requires": _MASTERED, "capacity": 2, "focus": 0,
              "termination": {"pulses": 3, "interval": 20}, "name": "Pulse",
              "text": "Acts three times, a second apart, where it first landed. Its operations cost Focus for "
                      "each time."},
    "allies": {"slot": "selection", "requires": _MASTERED, "capacity": 2, "focus": 2,
               "selection": {"pick": "allies", "radius": 4, "targets": 4}, "name": "Allies",
               "text": "Acts on you and the members of your party nearest to where the spell lands. Only helpful "
                       "operations may choose allies."},
    # Authored words: invocations are written with these, and players cannot compose with them (roadmap step 10).
    "kindle_light": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 1, "focus": 3,
                     "operation": {"effect": "illumination", "intent": "helpful", "principle": "radiance",
                                   "duration": KINDLE_MOTE_STEPS * MOTE_STEP_TICKS},
                     "name": "Kindled Light", "text": "Kindle's mote: steady light for a minute."},
    "dawn_aegis": {"slot": "operation", "requires": _UNDERSTOOD, "authored": True, "capacity": 2, "focus": 5,
                   "operation": {"effect": "protection", "intent": "helpful", "principle": "radiance", "magnitude": 8,
                                 "duration": 200},
                   "name": "Dawn Aegis", "text": "Dawn Aegis's shell of light (Absorption)."},
    "survey": {"slot": "selection", "requires": _UNDERSTOOD, "authored": True, "capacity": 2, "focus": 2,
               "selection": {"pick": "creatures", "radius": 6, "targets": 6}, "name": "Survey",
               "text": "Revelation's sweep: the creatures nearest to you."},
    "lance_beam": {"slot": "operation", "requires": _MASTERED, "authored": True, "capacity": 2, "focus": 4,
                   "operation": {"effect": "damage", "intent": "harmful", "principle": "radiance", "magnitude": 5,
                                 "school": "spell_power:arcane", "scaling": 0.5},
                   "name": "Lance", "text": "The Lance's beam: arcane damage that grows with arcane Spell Power."},
    "flash": {"slot": "operation", "requires": _MASTERED, "authored": True, "capacity": 2, "focus": 4,
              "operation": {"effect": "movement", "intent": "helpful", "principle": "radiance", "magnitude": 12},
              "name": "Flash", "text": "Flashstep's push: forward, the way you face."},
    "mend": {"slot": "operation", "requires": _MASTERED, "authored": True, "capacity": 2, "focus": 2,
             "operation": {"effect": "restoration", "intent": "helpful", "principle": "radiance", "magnitude": 4},
             "name": "Mend", "text": "Lanternward's healing light."},
}
COMPONENTS.update(ember.COMPONENTS)
COMPONENT_DATA_KEYS = ("slot", "requires", "authored", "capacity", "focus", "delivery", "selection", "operation",
                       "modifier", "termination")
for _component in COMPONENTS.values():
    _operation = _component.get("operation")
    if _operation and _operation["effect"] == "damage":
        assert _operation["school"] == PRINCIPLES[_operation["principle"]]["school"]

# The carrier spell an inscribed instrument casts (Java: ComposedSpells). Spell Engine owns its cast time and
# gestures; the composition decides everything else.
COMPOSED_CAST_SECONDS = 0.5
COMPOSED_BASE_COOLDOWN_SECONDS = 0.5
COMPOSE_RATE_TICKS = 20  # the most often a player may check or inscribe a composition
# Example compositions for the codex; the game tests compile the same texts.
COMPOSE_EXAMPLES = {
    "touch struck light": "Light where you touch",
    "here struck ward": "Ward yourself",
    "ray struck sear then here creatures dazzle": "Sear what you aim at, then dazzle what stands round it",
}


def composition_cost(text):
    """The Focus, capacity, targets, work and cooldown of a valid composition, as Java's Compiler works them out
    (used for the codex examples; tools/check_mod_data.py compares it with the Java constants)."""
    focus = capacity = targets = work = linger = 0
    for level in text.split(" then "):
        words = level.split()
        delivery, selection = COMPONENTS[words[0].split("+")[0]], COMPONENTS[words[1].split("+")[0]]
        ending = COMPONENTS[words[-1]] if COMPONENTS.get(words[-1], {}).get("slot") == "termination" else None
        pulses = ending["termination"]["pulses"] if ending else 1
        for word in words:
            names = word.split("+")
            part = COMPONENTS[names[0]]
            mods = [COMPONENTS[n] for n in names[1:]]
            capacity += part["capacity"] + sum(m["capacity"] for m in mods)
            if part["slot"] == "operation":
                focus += (part["focus"] + sum(m["focus"] for m in mods)) * pulses
                work += pulses * EFFECT_KINDS[part["operation"]["effect"]]["work"] * selection["selection"]["targets"]
            else:
                focus += part["focus"] + sum(m["focus"] for m in mods)
        targets += selection["selection"]["targets"]
        work += 0 if delivery["delivery"]["form"] == "here" else 1
        linger = max(linger, (pulses - 1) * (ending["termination"]["interval"] if ending else 0))
    focus = max(1, focus)
    cooldown = max(min(COMPOSE_LIMITS["MIN_COOLDOWN"] + 5 * focus, COMPOSE_LIMITS["MAX_COOLDOWN"]),
                   linger + COMPOSE_LIMITS["MIN_COOLDOWN"])
    return {"focus": focus, "capacity": capacity, "targets": targets, "work": work, "cooldown": cooldown}


ASPECT_SLOTS = {"magnitude": "operation", "duration": "operation", "radius": "selection", "range": "delivery"}
MAGNITUDE_EFFECTS = ("damage", "restoration", "movement", "status", "protection")


def _changes(aspect, part):
    """Whether a modifier of this aspect changes this component (Java: Compiler.changes)."""
    if part["slot"] != ASPECT_SLOTS[aspect]:
        return False
    if aspect == "magnitude":
        return part["operation"]["effect"] in MAGNITUDE_EFFECTS
    if aspect == "duration":
        return part["operation"].get("duration", 0) > 0
    if aspect == "radius":
        return part["selection"]["radius"] > 0
    return part["delivery"]["range"] > 0


def tuned_text(text, modifier):
    """The composition with a tuning's modifier joined to the first word it changes (Java: Compiler.tune)."""
    aspect = COMPONENTS[modifier]["modifier"]["aspect"]
    levels = [level.split() for level in text.split(" then ")]
    for words in levels:
        for i, word in enumerate(words):
            names = word.split("+")
            room = len(names) - 1 < COMPOSE_LIMITS["MAX_MODIFIERS"]
            if _changes(aspect, COMPONENTS[names[0]]) and modifier not in names[1:] and room:
                words[i] = word + "+" + modifier
                return " then ".join(" ".join(w) for w in levels)
    return None


def form_numbers(text):
    """What a (single-level) composition does, with its modifiers applied: reach, radius, and each operation's
    strength and time, as Java's Compiler works them out."""
    out = {"range": 0, "radius": 0, "targets": 1, "operations": []}
    for word in text.split():
        names = word.split("+")
        part, mods = COMPONENTS[names[0]], [COMPONENTS[n]["modifier"] for n in names[1:]]
        if part["slot"] == "delivery":
            out["range"] = part["delivery"]["range"] + sum(m["amount"] for m in mods if m["aspect"] == "range")
        elif part["slot"] == "selection":
            out["radius"] = part["selection"]["radius"] + sum(m["amount"] for m in mods if m["aspect"] == "radius")
            out["targets"] = part["selection"]["targets"]
        elif part["slot"] == "operation":
            op = part["operation"]
            magnitude, duration = op.get("magnitude", 0), op.get("duration", 0)
            for m in mods:
                if m["aspect"] == "magnitude":
                    magnitude += max(1, op.get("magnitude", 0) * m["amount"] // 100)
                elif m["aspect"] == "duration":
                    duration += op.get("duration", 0) * m["amount"] // 100
            out["operations"].append({"name": names[0], "effect": op["effect"], "magnitude": magnitude,
                                      "duration": duration, "scaling": op.get("scaling", 0),
                                      "school": op.get("school"), "status": op.get("status")})
    return out


def invocation_forms(key):
    """An invocation untuned (None) and with each tuning: {tuning: (text, cost, numbers)}."""
    info = INVOCATIONS[key]
    forms = {None: (info["composition"], composition_cost(info["composition"]), form_numbers(info["composition"]))}
    for tuning in info["tunings"]:
        is_modifier = COMPONENTS.get(tuning, {}).get("slot") == "modifier"
        text = tuned_text(info["composition"], tuning) if is_modifier else None
        # A tuning that is not a modifier or changes nothing has no form (tools/check_mod_data.py reports it).
        forms[tuning] = (text, composition_cost(text), form_numbers(text)) if text else (None, None, None)
    return forms


def persistence(numbers):
    """The most ticks anything a single-level form makes lasts (Java: Plan.persists)."""
    return max([op["duration"] for op in numbers["operations"]] + [0])


# Statuses invocations give, in the codex's words.
STATUS_NAMES = dict(ember.STATUS_NAMES)


def school_name(school):
    """A Spell Power school in the codex's words ("arcane", "fire"); plain magic when it names none."""
    return school.split(":")[1] if school else "magic"


def operation_summary(op):
    """One operation's effect in the codex's words."""
    effect = op["effect"]
    if effect == "damage":
        school = school_name(op.get("school"))
        scaled = f" (+{op['scaling']} per point of {school} Spell Power)" if op["scaling"] else ""
        return f"{op['magnitude']} {school} damage{scaled}"
    if effect == "restoration":
        return f"restores {op['magnitude']} health"
    if effect == "protection":
        return f"absorbs up to {4 * max(1, (op['magnitude'] + 3) // 4)} damage for {seconds(op['duration'])} seconds"
    if effect == "movement":
        return f"a push of {op['magnitude'] / 10} blocks a tick"
    if effect == "detection":
        return f"glowing for {seconds(op['duration'])} seconds"
    if effect == "illumination":
        return f"light for {seconds(op['duration'])} seconds"
    if effect == "status":
        return f"{STATUS_NAMES.get(op['status'], op['status'])} for {seconds(op['duration'])} seconds"
    if effect == "alteration":
        return "kindles a hearth" if op.get("school") == ember.COMPONENTS["kindling"]["operation"]["school"] else "puts out fire"
    return effect


def invocation_codex():
    """The Invocations category: an overview and one entry per invocation, from INVOCATIONS and the same compiled
    numbers the server runs."""
    entries = {
        ("invocations", "overview"): {
            "name": "Invocations", "x": 0, "y": 0, "icon": f"{MOD}:initiate_wand", "condition": "understood",
            "description": "Spells the traditions wrote down",
            "pages": [
                ("text", "Invocations",
                 "An invocation is a spell someone wrote before you, in the same words you compose with and under "
                 "the same limits. You learn it from research, not from an item: an instrument only lets you cast "
                 "what you have learned. Each has one **role**: damage, defense, movement, support, investigation "
                 "or utility."),
                ("text", "Tuning",
                 "An invocation can be **tuned** with one modifier from the short list it allows, if you could use "
                 "that modifier yourself. Tuning changes its numbers, never what it does, and costs that modifier's "
                 "Focus with every cast. Type `/jugcraft concordance tune` with an instrument in hand to see your "
                 "tunings, `tune <invocation> <modifier>` to set one and `tune <invocation> clear` to remove it."),
            ],
        },
    }
    # First Light's invocations, then Hearthbinding's in a row below them.
    positions = {("first_light", "understood"): [(2, 0), (2, 2), (0, 2)],
                 ("first_light", "mastered"): [(4, 0), (4, 2), (4, 4)],
                 ("hearthbinding", "understood"): [(0, 6), (2, 6), (4, 6)], ("hearthbinding", "mastered"): [(6, 6)]}
    for key, info in INVOCATIONS.items():
        research = info["research"].split(":")[1]
        teacher = RESEARCH[research]["name"]
        x, y = positions[(research, info["stage"])].pop(0)
        forms = invocation_forms(key)
        text, cost, numbers = forms[None]
        reach = f"Reach {numbers['range']} blocks. " if numbers["range"] else ""
        if numbers["radius"]:
            reach += f"Up to {numbers['targets']} within {numbers['radius']} blocks. "
        effects = "; ".join(operation_summary(op) for op in numbers["operations"])
        cast = f"cast {info['cast']:g} seconds" if info["cast"] else "instant"
        stats = (f"**{INVOCATION_ROLES[info['role']]}.** {reach}{effects[0].upper() + effects[1:]}.\\\n\\\n"
                 f"{info['focus']} Focus ({info['mastered_focus']} once {teacher} is mastered), {cast}, cooldown "
                 f"{info['cooldown']:g} seconds.")
        tunings = []
        for tuning in info["tunings"]:
            t_text, t_cost, t_numbers = forms[tuning]
            if t_text is None:
                continue
            extra = t_cost["focus"] - cost["focus"]
            aspect = COMPONENTS[tuning]["modifier"]["aspect"]
            if aspect == "range":
                what = f"reach {t_numbers['range']} blocks"
            elif aspect == "radius":
                what = f"{t_numbers['radius']} blocks round"
            else:
                what = "; ".join(operation_summary(op) for op in t_numbers["operations"])
            tunings.append(f"- **{COMPONENTS[tuning]['name']}**: {what} (+{extra} Focus)")
        learned = f"Learned when {teacher} is {info['stage']}."
        condition = info["stage"] if research == "first_light" else (research, info["stage"])
        entries[("invocations", key)] = {
            "name": info["name"], "x": x, "y": y, "icon": info["icon"], "condition": condition,
            "description": f"{INVOCATION_ROLES[info['role']]}: {info['description']}",
            "pages": [
                ("text", info["name"], f"{info['description']}\\\n\\\n{stats} {learned}"),
                ("text", "Tuning and Counters",
                 "Tunings:\\\n" + "\\\n".join(tunings) + f"\\\n\\\n**Counter.** {info['counter']}"),
            ],
        }
    return entries


for _working in WORKINGS.values():
    _working.update({"station": f"{MOD}:lampwright_bench", "research": f"{MOD}:first_light", "stage": "understood"})

SCHEMA = 1


def items():
    return list(ITEMS)


def blocks():
    return list(BLOCKS)


def itemless_blocks():
    return list(ITEMLESS_BLOCKS)


def research_advancement(research, state):
    """The advancement Jugcraft awards when a player's research reaches a state (Java: ConcordanceProgress.award)."""
    return f"concordance_{research}_{state}"


def advancement_ids():
    return ["concordance", "concordance_first_kindle"] + [research_advancement(r, s) for r in RESEARCH
                                                          for s in RESEARCH_STAGES]


# Jugcraft items with their own weapon arts or uses: Spell Engine's fallback would otherwise make them spell casters
# and give some of them Spell Engine weapon skills (docs/ARCANE_CONCORDANCE.md, "Equipment defaults").
def spell_opt_outs():
    import arms
    import arms_variants
    return sorted(set(arms.items()) | set(arms_variants.items())
                  | {"power_katana", "power_bow", "ronin_katana", "carnival_mallet", "pinata_stick"})


# ------------------------------------------------------------------------------------------------------- codex text

def seconds(ticks):
    value = ticks / 20
    return str(int(value)) if value == int(value) else f"{value:g}"


def specimen_name(ref):
    names = {"minecraft:amethyst_shard": "Amethyst Shard", "minecraft:glowstone_dust": "Glowstone Dust",
             f"{MOD}:glowcap": "Glowcap", f"{MOD}:glimmerbloom": "Glimmerbloom",
             f"{MOD}:jack_o_lantern_mushroom": "Jack-o'-Lantern Mushroom", "minecraft:glow_ink_sac": "Glow Ink Sac",
             "minecraft:glow_lichen": "Glow Lichen", "minecraft:glow_berries": "Glow Berries"}
    return names[ref]


def specimen_table():
    return "\\\n".join(f"- {specimen_name(ref)}: {value} Radiance" for ref, value in SPECIMENS.items())


# Codex entries: (category, entry) -> {name, description, x, y, icon, condition (research state or None), pages}.
# A page is (type, title, text[, extra]) with an optional condition. Texts are lang strings; Modonomicon renders them
# as markdown. Every number comes from the tables above.
def codex():
    principles = "\\\n".join(f"- **{p['name']}**: {p['domain']}" for p in PRINCIPLES.values())
    resources = "\\\n".join(f"- **{r['name']}** ({r['kind']})" for r in RESOURCES.values())
    stages = " > ".join(s["name"] for s in STAGES.values())
    return {
        ("foundations", "concordance"): {
            "name": "The Arcane Concordance", "x": 0, "y": 0, "icon": "minecraft:writable_book", "condition": None,
            "description": "What this codex records, and how to begin",
            "pages": [
                ("text", "The Arcane Concordance",
                 "The Concordance is the practice of working with the Principles that run through the world. It "
                 "begins with **looking closely**. Nothing in this codex teaches by being read: you learn by "
                 "examining, studying and doing, and the codex records what you have learned."),
                ("text", "Where to Begin",
                 "Find something that holds its own light. Amethyst grows in geodes underground; glow lichen and "
                 "glow berries hang in caves.\\\n\\\n**Sneak and use** a luminous specimen to examine it. Its glow "
                 "is easiest to judge **in darkness**: a dark cave, or the open sky at night."),
                ("text", "The Path", f"The Concordance has five stages: {stages}. Each opens new practices; no one "
                                     f"needs to follow every tradition."),
            ],
        },
        ("foundations", "principles"): {
            "name": "Principles and Resources", "x": 2, "y": 0, "icon": "minecraft:amethyst_cluster",
            "condition": None, "description": "The ten Principles and the seven resources",
            "pages": [
                ("text", "The Ten Principles", principles),
                ("text", "Resources Are Not Interchangeable",
                 f"{resources}\\\n\\\nNothing turns one into another except a recipe that names both, with its loss."),
                ("text", "Knowledge", "Knowledge is not a resource. A research entry moves through four states, "
                                      "**Encountered**, **Observed**, **Understood** and **Mastered**, from evidence "
                                      "you gather yourself. Repeating the same observation adds nothing."),
            ],
        },
        ("foundations", "focus"): {
            "name": "Focus", "x": 0, "y": 2, "icon": f"{MOD}:initiate_wand", "condition": None,
            "description": "Your own capacity to cast",
            "pages": [
                ("text", "Focus",
                 f"Every invocation spends **Focus**. You hold at most {FOCUS_MAX} points; one returns every "
                 f"{seconds(FOCUS_REGEN_TICKS)} seconds, whatever you are doing. Focus is yours alone: no item, "
                 f"device or trade can store it.\\\n\\\nWhile you hold an instrument, your Focus shows at the "
                 f"bottom right of the screen."),
            ],
        },
        ("foundations", "initiate_wand"): {
            "name": "Initiate's Wand", "x": 2, "y": 2, "icon": f"{MOD}:initiate_wand", "condition": None,
            "description": "The first instrument",
            "pages": [
                ("crafting_recipe", "Initiate's Wand",
                 "An instrument channels invocations. Hold it in your **main hand**; the invocations you have "
                 "understood appear on the spell bar, and the first is cast with the use key.", f"{MOD}:initiate_wand"),
                ("text", "Casting",
                 "The spell bar lists your invocations in order. The **use key** casts the first; the number keys "
                 "**2**, **3** and on cast the ones after it. While the instrument is in your hand those number keys "
                 "cast instead of changing the hotbar slot: scroll to change slot, or bind Spell Engine's own spell "
                 "keys in Controls to free them."),
            ],
        },
        ("foundations", "research_notes"): {
            "name": "Research Notes", "x": 0, "y": 4, "icon": f"{MOD}:research_notes", "condition": None,
            "description": "Sharing what you know",
            "pages": [
                ("crafting_recipe", "Research Notes",
                 "**Use** a blank sheet to write down every research entry you have begun, as far as you have come "
                 "(up to understood). Anyone else can **use** your notes to read them; reading does not use them "
                 "up.", f"{MOD}:research_notes"),
                ("text", "What Notes Can Teach",
                 "Notes are a shared record, not experience. Where an entry accepts notes, reading them can stand in "
                 "for one way of understanding it, but you must still encounter and observe it **yourself**, and "
                 "mastery comes only from your own practice. Notes from the same writer count once, and your own "
                 "notes teach you nothing new."),
            ],
        },
        ("radiance", "first_light"): {
            "name": "First Light", "x": 0, "y": 0, "icon": "minecraft:amethyst_shard", "condition": None,
            "description": "Radiance: research",
            "pages": [
                ("text", "First Light",
                 "Some things hold light of their own. **Sneak and use** one to examine it.\\\n\\\nLuminous "
                 "specimens:\\\n" + "\\\n".join(f"- {specimen_name(ref)}" for ref in SPECIMENS)),
                ("text", "Encountered", "You have handled a luminous specimen and noticed its glow. Now judge it "
                                        "where there is no other light.", "encountered"),
                ("text", "Observed",
                 f"In darkness (light {DARK_LIGHT} or less) the glow is the specimen's own. To **understand** it, "
                 f"study a specimen at a **Lampwright's Bench**, examine {FIELD_SPECIMENS} different specimens in "
                 f"darkness, or read **Research Notes** from someone who has understood it.", "observed"),
                ("text", "Understood",
                 f"Radiance can be gathered and set loose. You can now cast **Kindle**, **Dawn Aegis** and "
                 f"**Revelation** with an instrument, and kindle, infuse and channel lanterns at a Lampwright's "
                 f"Bench.\\\n\\\nTo **master** First Light, Kindle light in {MASTERY_CHUNKS} different chunks.",
                 "understood"),
                ("text", "Mastered", "You can now cast **Lance of Dawn**, **Flashstep** and **Lanternward**, and "
                                     "every First Light invocation costs less Focus (see Invocations).",
                 "mastered"),
            ],
        },
        ("radiance", "lampwright_bench"): {
            "name": "Lampwright's Bench", "x": 2, "y": 0, "icon": f"{MOD}:lampwright_bench", "condition": None,
            "description": "Study specimens and tend lanterns",
            "pages": [
                ("crafting_recipe", "Lampwright's Bench",
                 "The bench holds one specimen and one piece of work. Its buttons say what they need when they "
                 "cannot run.", f"{MOD}:lampwright_bench"),
                ("text", "Study",
                 f"**Study** reads a specimen for {seconds(STUDY_TICKS)} seconds. The specimen is held while it "
                 f"is read and used up only when the study finishes; cancel, break the bench or take the specimen "
                 f"and nothing is lost. If you are away when it finishes, the bench keeps your notes until you "
                 f"next open it."),
                ("text", "Lanterns",
                 f"Once First Light is understood: **Kindle** turns a lantern and an amethyst shard into a Kindled "
                 f"Lantern with {LANTERN_START} Radiance. **Infuse** adds a specimen's Radiance to a Kindled "
                 f"Lantern, up to {LANTERN_CAPACITY}; a specimen that would overfill it is refused. **Channel** "
                 f"spends {CHANNEL_FOCUS} Focus for {CHANNEL_RADIANCE} Radiance.\\\n\\\n" + specimen_table()),
            ],
        },
        ("radiance", "lumen_sconce"): {
            "name": "Lumen Sconce", "x": 0, "y": 4, "icon": f"{MOD}:lumen_sconce", "condition": "understood",
            "description": "Light that stays",
            "pages": [
                ("crafting_recipe", "Lumen Sconce",
                 f"A brass stand that burns Radiance for a steady light 15: one measure every "
                 f"{seconds(SCONCE_BURN_TICKS)} seconds, up to {LANTERN_CAPACITY} measures.", f"{MOD}:lumen_sconce"),
                ("text", "Pouring and Drawing",
                 f"Use it with a **Kindled Lantern** to pour up to {SCONCE_POUR} Radiance in; what does not fit "
                 f"stays in the lantern. Anyone may pour, so a Lampwright can keep a town's lamps lit. **Sneak** to "
                 f"draw Radiance back into your lantern: only the sconce's owner may. Only Radiance burns in a "
                 f"sconce, and broken, it keeps what it held, less the measure it was burning."),
            ],
        },
        ("radiance", "kindled_lantern"): {
            "name": "Kindled Lantern", "x": 2, "y": 2, "icon": f"{MOD}:kindled_lantern", "condition": "understood",
            "description": "Light that travels with you",
            "pages": [
                ("text", "Kindled Lantern",
                 f"Use it to light it or put it out. Lit, it burns one Radiance every "
                 f"{seconds(LANTERN_BURN_TICKS)} seconds whether carried or not, and while it is in either hand the "
                 f"air round you is lit (light {TRAIL_LIGHT}). Put out, it has spent the measure it was burning. It holds up "
                 f"to {LANTERN_CAPACITY} Radiance.\\\n\\\n"
                 f"A lantern can be given or traded; anyone may carry its light, but only someone who understands "
                 f"First Light can recharge it."),
            ],
        },
        **composition_codex(),
        **invocation_codex(),
        **rituals.codex(INSTRUMENT_LIMITS["initiate_wand"]),
        **alchemy.codex(),
        **ecology.codex(),
        **celestial.codex(),
        **crimson.codex(),
        **workers.codex(),
        **logistics.codex(),
        **artifice.codex(),
        **relics.codex(),
        **equivalence.codex(),
        **hexes.codex(),
        **conclave.codex(),
        **progression.codex(),
        **spire.codex(),
        **ember.codex(),
        **trinkets.codex(),
        **journal.codex(),
        **signs.codex(),
    }


def component_numbers(info):
    """One component's numbers, as the codex states them (the same data the server loads)."""
    if "delivery" in info:
        reach = info["delivery"]["range"]
        return f"Reach: {reach} blocks." if reach else "No reach: it lands at once."
    if "selection" in info:
        sel = info["selection"]
        if sel["pick"] == "struck":
            return "One target."
        what = {"creatures": "creatures", "blocks": "blocks", "allies": "of you and your party"}[sel["pick"]]
        return f"Up to {sel['targets']} {what} within {sel['radius']} blocks."
    if "operation" in info:
        op = info["operation"]
        effect = op["effect"]
        if effect == "damage":
            scaling = f", and {op['scaling']} more per point of Spell Power" if op.get("scaling") else ""
            return f"{op['magnitude']} damage{scaling}."
        if effect == "restoration":
            return f"Restores {op['magnitude']} health."
        if effect == "movement":
            return f"A push of {op['magnitude'] / 10} blocks a tick."
        if effect == "protection":
            amplifier = max(0, (op["magnitude"] + 3) // 4 - 1)
            return f"Absorbs up to {4 * (amplifier + 1)} damage for {seconds(op['duration'])} seconds."
        if effect == "status":
            return f"Level {op['magnitude'] + 1} for {seconds(op['duration'])} seconds."
        return f"Lasts {seconds(op['duration'])} seconds."
    if "modifier" in info:
        mod = info["modifier"]
        unit = "%" if mod["aspect"] in ("magnitude", "duration") else " blocks"
        return f"+{mod['amount']}{unit} {ASPECT_NAMES[mod['aspect']]}."
    term = info["termination"]
    return f"{term['pulses']} times, every {seconds(term['interval'])} seconds."


ASPECT_NAMES = {"magnitude": "strength", "duration": "time", "radius": "radius", "range": "range"}
SLOT_ENTRIES = {"delivery": ("deliveries", "Deliveries", "How a spell leaves you", 2, 0, "minecraft:spectral_arrow"),
                "selection": ("selections", "Selections", "What a spell chooses", 2, 2, "minecraft:spyglass"),
                "operation": ("operations", "Operations", "What a spell does", 0, 2, "minecraft:glowstone_dust"),
                "modifier": ("modifiers", "Modifiers", "Changing a word", 0, 4, "minecraft:redstone"),
                "termination": ("endings", "Endings", "How a spell ends", 2, 4, "minecraft:clock")}


def composition_codex():
    """The Composition category: how to write a spell, and one page per component, generated from COMPONENTS."""
    wand = INSTRUMENT_LIMITS["initiate_wand"]
    examples = "\\\n".join(f"- `{text}`: {note} ({composition_cost(text)['focus']} Focus)"
                             for text, note in COMPOSE_EXAMPLES.items())
    entries = {
        ("composition", "composing"): {
            "name": "Composing Spells", "x": 0, "y": 0, "icon": "minecraft:writable_book", "condition": "understood",
            "description": "Writing spells of your own",
            "pages": [
                ("text", "Composing Spells",
                 f"Once First Light is understood you can write spells of your own. A spell is a few words in "
                 f"order: a **delivery** (how it leaves you), a **selection** (what it chooses where it lands), one "
                 f"to {COMPOSE_LIMITS['MAX_OPERATIONS']} **operations** (what it does) and an optional **ending**. "
                 f"Join a **modifier** to the word it changes with **+**, and write **then** before a second spell "
                 f"that starts where the first landed."),
                ("text", "Writing and Inscribing",
                 "Hold an instrument and type `/jugcraft concordance compose check` and the spell to see what it "
                 "does and costs, or exactly why it cannot work. `compose inscribe` writes it on the instrument and "
                 "it appears on your spell bar; `compose show` reads it back and `compose clear` wipes it."),
                ("text", "Examples", examples),
                ("text", "Limits",
                 f"The Initiate's Wand holds {wand['capacity']} capacity of words. A spell on it reaches at most "
                 f"{wand['targets']} different targets, does at most {wand['work']} work, branches at most "
                 f"{wand['branches']} time and lasts at most {seconds(wand['duration'])} seconds.\\\n\\\n"
                 f"A spell costs the Focus of its words (an operation's for every pulse), taken once when it takes "
                 f"effect: a spell that does nothing costs nothing. Its cooldown is half a second and a quarter second "
                 f"for each Focus, and lasts at least as long as it pulses."),
            ],
        },
    }
    for slot, (key, name, description, x, y, icon) in SLOT_ENTRIES.items():
        pages = []
        for component, info in COMPONENTS.items():
            if info["slot"] != slot or info.get("authored"):
                continue
            needs = f"Needs First Light {info['requires']['state']}. Capacity {info['capacity']}, Focus {info['focus']}."
            pages.append(("text", info["name"], f"`{component}`: {info['text']}\\\n\\\n{component_numbers(info)} {needs}"))
        entries[("composition", key)] = {"name": name, "x": x, "y": y, "icon": icon, "condition": "understood",
                                         "description": description, "pages": pages}
    return entries


CATEGORIES = {
    "foundations": {"name": "Foundations", "icon": "minecraft:amethyst_shard", "sort": 0,
                    "description": "The Concordance, its Principles and its resources"},
    "radiance": {"name": "Radiance", "icon": "minecraft:glowstone_dust", "sort": 1,
                 "description": "The Lampwrights' practice"},
    "composition": {"name": "Composition", "icon": "minecraft:writable_book", "sort": 2,
                    "description": "Writing spells of your own"},
    "invocations": {"name": "Invocations", "icon": f"{MOD}:initiate_wand", "sort": 3,
                    "description": "Spells the traditions wrote down, and how to answer them"},
    **rituals.CATEGORY,
    **alchemy.CATEGORY,
    **ecology.CATEGORY,
    **celestial.CATEGORY,
    **crimson.CATEGORY,
    **workers.CATEGORY,
    **artifice.CATEGORY,
    **relics.CATEGORY,
    **equivalence.CATEGORY,
    **hexes.CATEGORY,
    **conclave.CATEGORY,
    **spire.CATEGORY,
    **ember.CATEGORY,
}

ENTRY_BACKGROUNDS = {None: "square_gray", "encountered": "square_gray", "observed": "square_gray",
                     "understood": "hexagon_purple", "mastered": "hexagon_gold"}

# ------------------------------------------------------------------------------------------------- data generation


def rid(path):
    return f"{MOD}:{path}"


def research_json(key, info):
    return {"schema": SCHEMA, "principle": info["principle"], "tradition": info["tradition"],
            "stage": info["stage"], "icon": info["icon"], "requires": info["requires"],
            "states": {state: {"any": rules} for state, rules in info["states"].items()},
            "unlocks": info["unlocks"]}


def invocation_json(key, info):
    return {"schema": SCHEMA, "spell": rid(key), "principle": info["principle"], "research": info["research"],
            "stage": info["stage"], "focus": info["focus"], "mastered_focus": info["mastered_focus"],
            "role": info["role"], "composition": info["composition"], "tunings": [rid(t) for t in info["tunings"]],
            "work": info["work"], "persists": info["persists"]}


def invocation_range(info):
    """The reach of an invocation's delivery, before tunings (0 for one that lands where you stand)."""
    return COMPONENTS[info["composition"].split()[0]]["delivery"]["range"]


def spell_json(key):
    """An invocation as a Spell Engine spell. Spell Engine owns the cast timeline, the gestures, the release sound and
    particles and its cooldown; Jugcraft's CASTING_ATTEMPT listener gates it (instrument, research, Focus), the CUSTOM
    impact jugcraft:invocation runs the invocation's compiled composition on the server, and Focus is spent once, in
    COST_CONSUME, after a cast that took effect (docs/ARCANE_CONCORDANCE.md, "One authority per cast")."""
    info = INVOCATIONS[key]
    cast_animation, release_animation = info["animation"]
    start_sound, release_sound = info["sounds"]
    cast = {"duration": info["cast"]}
    if cast_animation:
        cast["animation"] = {"id": rid(cast_animation)}
    if start_sound:
        cast["start_sound"] = {"id": rid(start_sound)}
    particles = [{"id": info["particle"], "appearance": {},
                  "batch": {"count": 12.0, "shape": "SPHERE", "anchor": "ENTITY", "vertical_origin": 0.6,
                            "min_speed": 0.05, "max_speed": 0.15}}]
    return {
        "school": PRINCIPLES[info["principle"]]["school"] or "spell_power:arcane",
        "range": float(invocation_range(info)),
        "tier": 1,
        "group": "concordance",
        "type": "ACTIVE",
        "active": {"cast": cast},
        "release": {"animation": {"id": rid(release_animation)}, "visuals": {"particles": particles},
                    "sound": {"id": rid(release_sound)}},
        "target": {"type": "CASTER"},
        "deliver": {"type": "DIRECT"},
        "impacts": [{"action": {"type": "CUSTOM", "custom": {"handler": rid("invocation"), "intent": "HELPFUL"}}}],
        "cost": {"exhaust": 0.0, "durability": 0,
                 "cooldown": {"duration": info["cooldown"], "hosting_item": False}},
    }


def composed_spell_json():
    """The carrier for inscribed spells (Java: ComposedSpells). Spell Engine owns its cast time, gestures and HUD; the
    CUSTOM impact compiles the instrument's inscription again on the server and runs the plan; Focus and the plan's
    own cooldown are settled once in COST_CONSUME. Its base cooldown only covers the moment before that."""
    return {
        "school": "spell_power:arcane",
        "range": float(COMPOSE_LIMITS["MAX_RANGE"]),
        "tier": 1,
        "group": "concordance",
        "type": "ACTIVE",
        "active": {"cast": {"duration": COMPOSED_CAST_SECONDS, "animation": {"id": rid("kindle_cast")},
                            "start_sound": {"id": rid("concordance.kindle_gather")}}},
        "release": {"animation": {"id": rid("kindle_release")}, "sound": {"id": rid("concordance.kindle")}},
        "target": {"type": "CASTER"},
        "deliver": {"type": "DIRECT"},
        "impacts": [{"action": {"type": "CUSTOM", "custom": {"handler": rid("composed"), "intent": "HELPFUL"}}}],
        "cost": {"exhaust": 0.0, "durability": 0,
                 "cooldown": {"duration": COMPOSED_BASE_COOLDOWN_SECONDS, "hosting_item": False}},
    }


def component_json(info):
    return {"schema": SCHEMA, **{key: info[key] for key in COMPONENT_DATA_KEYS if key in info}}


def player_animations():
    """Original Spell Engine/Player Animation Library clips in the PlayerAnimator "emote" format Spell Engine uses
    (angles in radians; a negative arm pitch raises the arm forward; parts and axes not listed keep their pose). No
    "torso" key: in this format PAL reads it as the whole-body bone, so it would tilt the player."""
    def clip(name, loop, begin, end, stop, keys):
        moves = []
        for tick, part, axis, value in keys:
            moves.append({"tick": tick, "easing": "EASEINOUTQUAD", "turn": 0, part: {axis: value}})
        return {"name": name, "author": "Jugcraft", "description": f"Arcane Concordance: {name}",
                "emote": {"isLoop": "true" if loop else "false", "returnTick": begin if loop else 0,
                          "beginTick": begin, "endTick": end, "stopTick": stop, "degrees": False, "moves": moves}}
    # Kindle, gathering: both hands cupped before the chest, rising and settling as the light gathers.
    cast = []
    for tick, lift in ((0, -1.10), (10, -1.28), (20, -1.10)):
        cast += [(tick, "rightArm", "pitch", lift), (tick, "rightArm", "yaw", -0.32), (tick, "rightArm", "roll", 0.12),
                 (tick, "leftArm", "pitch", lift), (tick, "leftArm", "yaw", 0.32), (tick, "leftArm", "roll", -0.12)]
    # Kindle, release: the casting hand opens upward and outward, then falls back.
    release = [(0, "rightArm", "pitch", -1.20), (3, "rightArm", "pitch", -2.05), (8, "rightArm", "pitch", -1.30),
               (0, "rightArm", "yaw", -0.30), (3, "rightArm", "yaw", 0.05), (8, "rightArm", "yaw", -0.10),
               (0, "rightArm", "roll", 0.10), (3, "rightArm", "roll", 0.28), (8, "rightArm", "roll", 0.10),
               (0, "leftArm", "pitch", -1.10), (3, "leftArm", "pitch", -0.55), (8, "leftArm", "pitch", -0.30),
               (0, "head", "pitch", 0.0), (3, "head", "pitch", -0.12), (8, "head", "pitch", 0.0)]
    clips = {"kindle_cast": clip("kindle_cast", True, 0, 20, 24, cast),
             "kindle_release": clip("kindle_release", False, 0, 8, 12, release)}

    def both(tick, pitch, yaw, roll):
        """Both arms, mirrored."""
        return [(tick, "rightArm", "pitch", pitch), (tick, "rightArm", "yaw", -yaw), (tick, "rightArm", "roll", roll),
                (tick, "leftArm", "pitch", pitch), (tick, "leftArm", "yaw", yaw), (tick, "leftArm", "roll", -roll)]
    # Lance, drawing: the casting arm cocked back at the shoulder, the other hand forward to sight along.
    lance_cast = []
    for tick, draw in ((0, -0.40), (10, -0.15), (20, -0.40)):
        lance_cast += [(tick, "rightArm", "pitch", draw), (tick, "rightArm", "yaw", 0.35), (tick, "rightArm", "roll", 0.20),
                       (tick, "leftArm", "pitch", -1.45), (tick, "leftArm", "yaw", 0.10)]
    # Lance, release: the arm thrusts straight out along the line of the beam and holds a moment.
    lance_release = [(0, "rightArm", "pitch", -0.40), (2, "rightArm", "pitch", -1.62), (9, "rightArm", "pitch", -1.40),
                     (0, "rightArm", "yaw", 0.35), (2, "rightArm", "yaw", -0.05), (9, "rightArm", "yaw", 0.0),
                     (0, "leftArm", "pitch", -1.45), (2, "leftArm", "pitch", -0.20), (9, "leftArm", "pitch", -0.10)]
    # Dawn Aegis, gathering: forearms crossed before the chest.
    aegis_cast = []
    for tick, pitch in ((0, -1.30), (10, -1.45), (20, -1.30)):
        aegis_cast += both(tick, pitch, -0.55, 0.0)
    # Dawn Aegis, release: the crossed arms fling wide and down, opening the shell.
    aegis_release = both(0, -1.30, -0.55, 0.0) + both(3, -1.10, 0.55, 0.70) + both(9, -0.30, 0.20, 0.25)
    # Revelation, gathering: a hand shading the eyes, the head turning slowly.
    reveal_cast = []
    for tick, turn in ((0, -0.35), (10, 0.35), (20, -0.35)):
        reveal_cast += [(tick, "rightArm", "pitch", -2.45), (tick, "rightArm", "yaw", -0.55), (tick, "head", "yaw", turn)]
    # Revelation, release: the hand sweeps out flat across the view.
    reveal_release = [(0, "rightArm", "pitch", -2.45), (4, "rightArm", "pitch", -1.55), (10, "rightArm", "pitch", -1.40),
                      (0, "rightArm", "yaw", -0.55), (4, "rightArm", "yaw", 0.20), (10, "rightArm", "yaw", 0.75),
                      (0, "head", "yaw", 0.0)]
    # Flashstep, release (no cast): both arms swept back as the body is carried forward.
    flash_release = both(0, 0.0, 0.0, 0.0) + both(2, 0.95, 0.15, 0.25) + both(8, 0.15, 0.0, 0.05)
    # Lanternward, gathering: both hands raised high, as if lifting a lantern for others to see by.
    ward_cast = []
    for tick, lift in ((0, -2.55), (10, -2.75), (20, -2.55)):
        ward_cast += both(tick, lift, 0.10, 0.15)
    # Lanternward, release: the arms open and lower outward, spreading the light.
    ward_release = both(0, -2.55, 0.10, 0.15) + both(4, -1.60, 0.40, 0.85) + both(10, -0.40, 0.15, 0.30)
    clips.update({
        "lance_cast": clip("lance_cast", True, 0, 20, 24, lance_cast),
        "lance_release": clip("lance_release", False, 0, 9, 13, lance_release),
        "aegis_cast": clip("aegis_cast", True, 0, 20, 24, aegis_cast),
        "aegis_release": clip("aegis_release", False, 0, 9, 13, aegis_release),
        "reveal_cast": clip("reveal_cast", True, 0, 20, 24, reveal_cast),
        "reveal_release": clip("reveal_release", False, 0, 10, 14, reveal_release),
        "flash_release": clip("flash_release", False, 0, 8, 12, flash_release),
        "ward_cast": clip("ward_cast", True, 0, 20, 24, ward_cast),
        "ward_release": clip("ward_release", False, 0, 10, 14, ward_release),
    })
    return clips


def book_json():
    return {"name": f"book.{MOD}.{BOOK}.name", "tooltip": f"book.{MOD}.{BOOK}.tooltip",
            "description": f"book.{MOD}.{BOOK}.description", "model": rid(BOOK), "generate_book_item": True,
            "creative_tab": "modonomicon:modonomicon", "display_mode": "node", "page_display_mode": "double_page",
            "show_recently_unlocked": True}


def node_id(state, research="first_light"):
    return rid(f"concordance/{research}_{state}")


def condition_json(condition):
    """A codex condition: None (always open), a state of First Light, or (research, state) for another entry."""
    if condition is None:
        return {"type": "modonomicon:none"}
    research, state = condition if isinstance(condition, tuple) else ("first_light", condition)
    key = state if research == "first_light" else f"{research}.{state}"
    return {"type": "modonomicon:research_node_unlocked", "node_id": node_id(state, research),
            "tooltip": {"translate": f"book.{MOD}.{BOOK}.locked.{key}"}}


def condition_state(condition):
    return condition[1] if isinstance(condition, tuple) else condition


def codex_files(lang):
    """The book, its categories, entries and pages, and the research bridge, as {relative path: json}."""
    files = {}
    base = f"modonomicon/books/{BOOK}"
    files[f"{base}/book.json"] = book_json()
    lang[f"book.{MOD}.{BOOK}.name"] = "The Arcane Concordance"
    lang[f"book.{MOD}.{BOOK}.tooltip"] = "A record of what you have learned of the Principles"
    lang[f"book.{MOD}.{BOOK}.description"] = "The codex of the Arcane Concordance"
    for state, text in (("encountered", "Examine a luminous specimen first"),
                        ("observed", "Examine a luminous specimen in darkness first"),
                        ("understood", "Understand First Light first"), ("mastered", "Master First Light first")):
        lang[f"book.{MOD}.{BOOK}.locked.{state}"] = text
    for key, info in CATEGORIES.items():
        files[f"{base}/categories/{key}.json"] = {
            "name": f"book.{MOD}.{BOOK}.{key}.name", "description": f"book.{MOD}.{BOOK}.{key}.description",
            "icon": info["icon"], "sort_number": info["sort"], "display_mode": "node"}
        lang[f"book.{MOD}.{BOOK}.{key}.name"] = info["name"]
        lang[f"book.{MOD}.{BOOK}.{key}.description"] = info["description"]
    for (category, entry), info in codex().items():
        prefix = f"book.{MOD}.{BOOK}.{category}.{entry}"
        pages = []
        for index, page in enumerate(info["pages"]):
            kind, title, text = page[0], page[1], page[2]
            page_id = f"p{index}"
            page_json = {"type": f"modonomicon:{kind}", "id": page_id}
            lang[f"{prefix}.{page_id}.title"] = title
            lang[f"{prefix}.{page_id}.text"] = text
            if kind == "text":
                page_json.update({"title": f"{prefix}.{page_id}.title", "text": f"{prefix}.{page_id}.text",
                                  "show_title_separator": True})
                if len(page) > 3:
                    page_json["condition"] = condition_json(page[3])
            else:
                page_json.update({"title1": f"{prefix}.{page_id}.title", "recipe_id_1": page[3],
                                  "text": f"{prefix}.{page_id}.text"})
            pages.append(page_json)
        files[f"{base}/entries/{category}/{entry}.json"] = {
            "type": "modonomicon:content", "id": rid(f"{category}/{entry}"), "category": rid(category),
            "x": info["x"], "y": info["y"], "name": f"{prefix}.name", "description": f"{prefix}.description",
            "icon": info["icon"],
            "background": {"sprite": "modonomicon:modonomicon/themes/default/node/entry_backgrounds/"
                                     + ENTRY_BACKGROUNDS[condition_state(info["condition"])], "width": 26, "height": 26},
            "condition": condition_json(info["condition"]), "pages": pages}
        lang[f"{prefix}.name"] = info["name"]
        lang[f"{prefix}.description"] = info["description"]
    # The research bridge: each state of each entry is a fact, granted by the advancement Java awards for it.
    folder = "modonomicon/research/concordance"
    pairs = [(key, state) for key, info in RESEARCH.items() for state in RESEARCH_STAGES if state in info["states"]]
    files[f"{folder}/facts.json"] = [{"id": node_id(state, key)} for key, state in pairs]
    files[f"{folder}/hooks.json"] = [
        {"id": rid(f"concordance/{key}_{state}_hook"), "trigger_type": "modonomicon:advancement",
         "trigger_target": rid(research_advancement(key, state)), "fact_id": node_id(state, key)}
        for key, state in pairs]
    files[f"{folder}/nodes.json"] = [{"id": node_id(state, key), "required_facts": [node_id(state, key)]}
                                     for key, state in pairs]
    for key, state in pairs:
        lang[f"research_node.{MOD}.concordance.{key}_{state}"] = f"{RESEARCH[key]['name']}: {state}"
    for key, info in RESEARCH.items():
        for state, text in info.get("locked", {}).items():
            lang[f"book.{MOD}.{BOOK}.locked.{key}.{state}"] = text
    return files


ADVANCEMENT_TEXT = {
    "encountered": ("A Glow of Its Own", "Examine a luminous specimen", "task"),
    "observed": ("Against the Dark", "Examine a luminous specimen in darkness", "task"),
    "understood": ("First Light", "Understand Radiance: study at a bench, or examine three specimens in the dark",
                   "goal"),
    "mastered": ("Lamplighter", f"Master First Light: Kindle light in {MASTERY_CHUNKS} different chunks", "challenge"),
}


def advancements(data, write, lang):
    """The Concordance tab: its root appears with the first luminous specimen; the rest are awarded by the server when
    research advances (Java: ConcordanceProgress.award), and Modonomicon turns them into the codex's research facts."""
    folder = data / MOD / "advancement"
    write(folder / "concordance.json", {
        "display": {"icon": {"id": "minecraft:amethyst_shard"},
                    "title": {"translate": f"advancements.{MOD}.concordance.title"},
                    "description": {"translate": f"advancements.{MOD}.concordance.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False,
                    "background": "minecraft:block/amethyst_block"},
        "criteria": {"has_specimen": {"trigger": "minecraft:inventory_changed",
                                      "conditions": {"items": [{"items": list(SPECIMENS)}]}}},
        "requirements": [["has_specimen"]]})
    lang[f"advancements.{MOD}.concordance.title"] = "The Arcane Concordance"
    lang[f"advancements.{MOD}.concordance.description"] = "Find something that holds its own light"
    parent = "concordance"
    for state in RESEARCH_STAGES:
        key = research_advancement("first_light", state)
        title, description, frame = ADVANCEMENT_TEXT[state]
        icon = {"encountered": "minecraft:amethyst_shard", "observed": "minecraft:glow_lichen",
                "understood": f"{MOD}:initiate_wand", "mastered": f"{MOD}:kindled_lantern"}[state]
        write(folder / f"{key}.json", {
            "parent": rid(parent),
            "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{MOD}.{key}.title"},
                        "description": {"translate": f"advancements.{MOD}.{key}.description"}, "frame": frame,
                        "show_toast": True, "announce_to_chat": frame != "task"},
            "criteria": {"done": {"trigger": "minecraft:impossible"}}})
        lang[f"advancements.{MOD}.{key}.title"] = title
        lang[f"advancements.{MOD}.{key}.description"] = description
        parent = key
    for research, info in RESEARCH.items():
        if research == "first_light":
            continue
        requirement = info["requires"][0]
        parent = research_advancement(requirement["research"].split(":")[1], requirement["state"])
        for state in RESEARCH_STAGES:
            if state not in info["states"]:
                continue
            key = research_advancement(research, state)
            title, description, frame, icon = info["advancements"][state]
            write(folder / f"{key}.json", {
                "parent": rid(parent),
                "display": {"icon": {"id": icon}, "title": {"translate": f"advancements.{MOD}.{key}.title"},
                            "description": {"translate": f"advancements.{MOD}.{key}.description"}, "frame": frame,
                            "show_toast": True, "announce_to_chat": frame != "task"},
                "criteria": {"done": {"trigger": "minecraft:impossible"}}})
            lang[f"advancements.{MOD}.{key}.title"] = title
            lang[f"advancements.{MOD}.{key}.description"] = description
            parent = key
    write(folder / "concordance_first_kindle.json", {
        "parent": rid(research_advancement("first_light", "understood")),
        "display": {"icon": {"id": "minecraft:glowstone_dust"},
                    "title": {"translate": f"advancements.{MOD}.concordance_first_kindle.title"},
                    "description": {"translate": f"advancements.{MOD}.concordance_first_kindle.description"},
                    "frame": "task", "show_toast": True, "announce_to_chat": False},
        "criteria": {"done": {"trigger": "minecraft:impossible"}}})
    lang[f"advancements.{MOD}.concordance_first_kindle.title"] = "Let There Be"
    lang[f"advancements.{MOD}.concordance_first_kindle.description"] = "Cast Kindle with an instrument"


# Sounds (tools/concordance_sounds.py draws the .ogg files): event -> subtitle.
SOUND_EVENTS = {
    "concordance.kindle_gather": "Light gathers",
    "concordance.kindle": "Light kindles",
    "concordance.examine": "Specimen examined",
    "concordance.study_complete": "Study completes",
    "concordance.lantern_ignite": "Lantern kindles",
    "concordance.lantern_snuff": "Lantern goes out",
    "concordance.aegis": "Shell of light forms",
    "concordance.revelation": "Hidden things shine",
    "concordance.lance": "Beam of light strikes",
    "concordance.flash": "Flash of movement",
    "concordance.lanternward": "Warding light spreads",
}


SOUND_EVENTS.update(rituals.SOUND_EVENTS)
SOUND_EVENTS.update(alchemy.SOUND_EVENTS)
SOUND_EVENTS.update(signs.SOUND_EVENTS)
SOUND_EVENTS.update(ember.SOUND_EVENTS)


def sounds():
    """Each event plays its drawn cue (tools/concordance_sounds.py), or the owner's recordings it is given instead
    (tools/concordance_ember.py SOUND_FILES), one picked at random."""
    return {event: {"subtitle": f"subtitles.{MOD}.{event}",
                    "sounds": [{"name": f"{MOD}:{name}", "attenuation_distance": 16}
                               for name in ember.SOUND_FILES.get(event, [f"concordance/{event.split('.', 1)[1]}"])]}
            for event in SOUND_EVENTS}


MESSAGES = {
    "examine.bright": "The %s's glow is lost in this light. Examine it somewhere dark.",
    "examine.dark": "In the dark, the %s glows with its own light.",
    "examine.known": "You have already observed the %s in the dark.",
    "research.encountered": "Research: %s encountered",
    "research.observed": "Research: %s observed",
    "research.understood": "Research: %s understood",
    "research.mastered": "Research: %s mastered",
    "invocation.not_allowed": "You may not change that block",
    "invocation.no_instrument": "%s does not fit this instrument",
    "tune.set": "%s is tuned with %s: %s Focus a cast",
    "tune.cleared": "%s is no longer tuned: %s Focus a cast",
    "tune.unknown": "You know no invocation called %s",
    "tune.not_offered": "%s cannot be tuned with %s; it takes %s",
    "tune.unlearned": "You have not learned %s yet",
    "tune.does_not_fit": "%s with %s does not fit this instrument",
    "tune.list": "%s: %s Focus, tuned with %s",
    "tune.list_untuned": "%s: %s Focus, untuned (takes %s)",
    "focus": "Focus %s / %s",
    "lantern.empty": "The lantern has no Radiance left",
    "lantern.lit": "Lantern lit: %s Radiance",
    "lantern.out": "Lantern put out: %s Radiance",
    "bench.busy": "The bench is already studying",
    "bench.no_specimen": "Put a specimen in the bench",
    "bench.not_specimen": "The bench cannot study that",
    "bench.nothing_to_learn": "There is nothing more to learn from that specimen",
    "bench.unknown_research": "You have not learned this working yet (the codex says how)",
    "bench.no_lantern": "Put a lantern in the work slot",
    "bench.no_kindled_lantern": "Put a Kindled Lantern in the work slot",
    "bench.no_amethyst": "Kindling a lantern needs an amethyst shard",
    "bench.overfull": "That would overfill the lantern",
    "bench.no_focus": "You need %s Focus",
    "bench.study_done": "Study of the %s complete",
    "bench.notes_waiting": "Your notes from the bench: %s",
    "bench.cancelled": "Study cancelled; nothing was used",
    "bench.disabled": "The Concordance is switched off on this server",
    "instrument.needed": "Hold a Concordance instrument in your main hand",
    "notes.nothing_to_write": "You have nothing to write down yet: examine a luminous specimen first",
    "notes.written": "Notes written on %s research entries",
    "notes.own": "These are your own notes",
    "notes.pending": "You read %s's notes; observe it for yourself and they will make sense",
    "notes.nothing_new": "%s's notes hold nothing new for you",
    "sconce.poured": "Poured %s Radiance: the sconce holds %s / %s",
    "sconce.drawn": "Drew %s Radiance: the sconce holds %s / %s",
    "sconce.full": "The sconce is full (%2$s / %3$s)",
    "sconce.lantern_full": "The lantern is full",
    "sconce.sconce_empty": "The sconce has no Radiance to draw",
    "sconce.lantern_empty": "The lantern has no Radiance to pour",
    "sconce.not_owner": "Only the sconce's owner may draw from it",
    "sconce.settling": "Let the light settle a moment",
    "sconce.wrong_type": "Only Radiance burns in a sconce",
    "sconce.status": "Lumen Sconce: %s / %s Radiance",
    "no_invocations": "You understand no invocation yet: examine luminous specimens in the dark, or study one at a "
                      "Lampwright's Bench",
    "disabled": "The Concordance is switched off on this server",
    **rituals.MESSAGES,
    **alchemy.MESSAGES,
    **ecology.MESSAGES,
    **celestial.MESSAGES,
    **crimson.MESSAGES,
    **workers.MESSAGES,
    **logistics.MESSAGES,
    **artifice.MESSAGES,
    **relics.MESSAGES,
    **equivalence.MESSAGES,
    **hexes.MESSAGES,
    **ember.MESSAGES,
    **conclave.MESSAGES,
    **progression.MESSAGES,
    **spire.MESSAGES,
}

SCREEN_TEXT = {
    "bench.study": "Study",
    "bench.cancel": "Cancel",
    "bench.kindle": "Kindle",
    "bench.infuse": "Infuse",
    "bench.channel": "Channel",
    "bench.specimen": "Specimen",
    "bench.work": "Work",
    "bench.progress": "Studying: %s%%",
    "bench.idle": "Idle",
    "bench.charge": "Radiance: %s / %s",
    "bench.help": "Hover a button to see what it needs",
    "focus.hud": "Focus %s/%s",
    "config.title": "Jugcraft: Arcane Concordance",
    "config.hud": "Show the Focus bar",
    "config.hud.tooltip": "Shows your Focus above the hotbar while you hold a Concordance instrument",
    "config.reduced_motion": "Reduced motion",
    "config.reduced_motion.tooltip": "Fewer and calmer particles from Kindled light and the bench",
}

TOOLTIPS = {
    "specimen": "Luminous specimen: sneak and use to examine it",
    "lantern.charge": "Radiance: %s / %s",
    "lantern.lit": "Lit",
    "lantern.unlit": "Put out",
    "jade.study": "Studying: %s%%",
    "jade.notes": "Notes waiting for their owner",
    **rituals.TOOLTIPS,
    **alchemy.TOOLTIPS,
    **ecology.TOOLTIPS,
    **celestial.TOOLTIPS,
    **crimson.TOOLTIPS,
    **workers.TOOLTIPS,
    **logistics.TOOLTIPS,
    **artifice.TOOLTIPS,
    **relics.TOOLTIPS,
    **equivalence.TOOLTIPS,
    **hexes.TOOLTIPS,
    **conclave.TOOLTIPS,
    **spire.TOOLTIPS,
}


# The compiler's problems and explanations (Java: concordance/compose, Text keys under compose.jugcraft.) and the
# compose command's replies. Every key the Java names must be here (tools/check_mod_data.py checks).
COMPOSE_TEXT = {
    "problem.empty": "Write a spell: a delivery, a selection and an operation, such as: touch struck light",
    "problem.too_long": "A spell is at most %s characters",
    "problem.character": "\"%s\" cannot appear in a spell: write component names, + and then",
    "problem.then_empty": "\"then\" needs a spell on both sides",
    "problem.empty_name": "\"%s\" has an empty name: put + only between names",
    "problem.bad_name": "\"%s\" is not a component name",
    "problem.too_many_names": "A spell names at most %s components",
    "problem.too_deep": "Branches nest at most %s deep",
    "problem.unknown": "There is no component called \"%s\"",
    "problem.loose_modifier": "%s is a modifier: join it with + to the word it changes",
    "problem.not_modifier": "%s is not a modifier, so it cannot be joined to %s",
    "problem.modifier": "%s changes %s, and %s has none to change",
    "problem.duplicate": "%s appears twice",
    "problem.too_many_modifiers": "%s has more than %s modifiers",
    "problem.research": "Using %s needs %s to be %s",
    "problem.expected": "%s is %s, but %s belongs here",
    "problem.after_termination": "%s comes after the ending %s: nothing follows an ending",
    "problem.missing": "The spell needs %s",
    "problem.too_many_operations": "A spell, or a branch, has at most %s operations",
    "problem.range": "%s would reach %s blocks; no spell reaches beyond %s",
    "problem.radius": "%s would spread %s blocks; no spell spreads beyond %s",
    "problem.selection": "%s acts on %s, which %s does not choose",
    "problem.harms_caster": "%s would harm you: here and struck reach only yourself",
    "problem.harms_allies": "%s would harm your allies: %s chooses only you and your party",
    "problem.authored": "%s belongs to an invocation and cannot be composed",
    "problem.magnitude": "%s would be %s strong; the most is %s",
    "problem.duration": "%s would last %s seconds; the %s allows %s",
    "problem.capacity": "This spell needs %s capacity; the %s holds %s",
    "problem.branches": "This spell branches %s times; the %s allows %s",
    "problem.targets": "This spell could reach %s targets; the %s allows %s",
    "problem.work": "This spell could do %s work; the %s allows %s",
    "problem.focus": "This spell would cost %s Focus; you can hold at most %s",
    "problem.branch_delivery": "A branch starts where the spell before it landed: begin it with here, not %s",
    "problem.no_instrument": "Hold a composing instrument in your main hand",
    "problem.too_fast": "Wait a moment before composing again",
    "explain.delivery.here": "%s: lands where you stand",
    "explain.delivery.here_then": "%s: lands where the spell before it landed",
    "explain.delivery.touch": "%s: lands on the first creature or surface within %s blocks",
    "explain.delivery.ray": "%s: flies to the first creature or surface within %s blocks",
    "explain.selection.struck": "%s: acts on the one thing it reached",
    "explain.selection.creatures": "%s: acts on up to %s creatures within %s blocks (never you, if harmful)",
    "explain.selection.blocks": "%s: acts on up to %s blocks within %s blocks",
    "explain.selection.allies": "%s: acts on you and up to %s of your party within %s blocks",
    "explain.scaling": "%s: %s more damage for each point of its school's Spell Power",
    "explain.operation.damage": "%s: deals %s damage",
    "explain.operation.restoration": "%s: restores %s health",
    "explain.operation.movement": "%s: pushes at %s blocks a tick, away from where the spell came (forward, if it "
                                  "came from where its target stands)",
    "explain.operation.illumination": "%s: sets light for %s seconds",
    "explain.operation.status": "%s: %s, level %s, for %s seconds",
    "explain.operation.interaction": "%s: uses the block as you would",
    "explain.operation.harvesting": "%s: gathers a ripe crop or plant",
    "explain.operation.protection": "%s: absorbs up to %s damage for %s seconds",
    "explain.operation.detection": "%s: makes creatures glow for %s seconds",
    "explain.operation.alteration": "%s: puts out fire",
    "explain.termination.pulse": "%s: acts %s times, every %s seconds, where it first landed",
    "explain.then": "Then, where it landed:",
    "explain.cost": "Costs %s Focus once it takes effect; cooldown %s seconds",
    "explain.limits": "At most %s targets and %s work; takes %s of the %s's %s capacity",
    "slot.delivery": "a delivery",
    "slot.selection": "a selection",
    "slot.operation": "an operation",
    "slot.modifier": "a modifier",
    "slot.termination": "an ending",
    "aspect.magnitude": "strength",
    "aspect.duration": "time",
    "aspect.radius": "a radius",
    "aspect.range": "a range",
    "on.creature": "creatures",
    "on.block": "blocks",
    "valid": "%s works:",
    "invalid": "%s cannot be cast:",
    "inscribed": "Inscribed on your %s: %s",
    "cleared": "The inscription is wiped",
    "nothing_inscribed": "Nothing is inscribed on this instrument",
    "inscription": "Inscribed: %s",
    "inscription_cost": "%s Focus, cooldown %s seconds",
}


RESEARCH_STATE_NAMES = {"none": "Not begun", "encountered": "Encountered", "observed": "Observed",
                        "understood": "Understood", "mastered": "Mastered"}


def lang_entries(lang):
    for item, info in ITEMS.items():
        lang[f"item.{MOD}.{item}"] = info["name"]
        lang[f"tooltip.{MOD}.{item}"] = info["tooltip"]
    lang[f"tooltip.{MOD}.research_notes.blank"] = "Blank: use it to write down what you know"
    lang[f"tooltip.{MOD}.research_notes.author"] = "Notes by %s"
    lang[f"tooltip.{MOD}.research_notes.entry"] = "%s: %s"
    for state, name in RESEARCH_STATE_NAMES.items():
        lang[f"research_state.{MOD}.{state}"] = name
    for block, info in {**BLOCKS, **ITEMLESS_BLOCKS}.items():
        lang[f"block.{MOD}.{block}"] = info["name"]
    lang[f"container.{MOD}.lampwright_bench"] = BLOCKS["lampwright_bench"]["name"]
    for key, info in INVOCATIONS.items():
        lang[f"spell.{MOD}.{key}.name"] = info["name"]
        teacher = RESEARCH[info["research"].split(":")[1]]["name"]
        lang[f"spell.{MOD}.{key}.description"] = (f"{INVOCATION_ROLES[info['role']]}. {info['description']} "
                                                  f"Costs {info['focus']} Focus ({info['mastered_focus']} once {teacher} "
                                                  f"is mastered).")
        lang[f"message.{MOD}.concordance.fizzle.{key}"] = info["fizzle"]
    for key, name in INVOCATION_ROLES.items():
        lang[f"role.{MOD}.{key}"] = name
    lang[f"tooltip.{MOD}.concordance.tuning"] = "%s: tuned with %s"
    for key, info in RESEARCH.items():
        lang[f"research.{MOD}.{key}"] = info["name"]
    for key, info in PRINCIPLES.items():
        lang[f"principle.{MOD}.{key}"] = info["name"]
    for key, info in RESOURCES.items():
        lang[f"resource.{MOD}.{key}"] = info["name"]
    for key, text in MESSAGES.items():
        lang[f"message.{MOD}.concordance.{key}"] = text
    for key, text in SCREEN_TEXT.items():
        lang[f"screen.{MOD}.concordance.{key}"] = text
    for key, text in TOOLTIPS.items():
        lang[f"tooltip.{MOD}.concordance.{key}"] = text
    for key, text in COMPOSE_TEXT.items():
        lang[f"compose.{MOD}.{key}"] = text
    for key, info in COMPONENTS.items():
        lang[f"component.{MOD}.{key}"] = info["name"]
        lang[f"component.{MOD}.{key}.description"] = info["text"]
    lang[f"spell.{MOD}.composed.name"] = "Inscribed Spell"
    lang[f"spell.{MOD}.composed.description"] = ("The spell inscribed on this instrument (see the codex, "
                                                 "Composition). Its cost and cooldown are its own.")
    for event, subtitle in SOUND_EVENTS.items():
        lang[f"subtitles.{MOD}.{event}"] = subtitle
    # Spell Engine names a missing "item" through its tag's translation key; Jugcraft's casting gate uses these two
    # pseudo-tags so its HUD says what is missing (Java: ConcordanceSpells.MISSING_*).
    lang[f"tag.item.{MOD}.concordance.focus"] = "Focus"
    lang[f"tag.item.{MOD}.concordance.instrument"] = "Concordance instrument"
    lang[f"tag.item.{MOD}.luminous_specimens"] = "Luminous Specimens"
    lang[f"tag.item.{MOD}.concordance_specimens"] = "Concordance Specimens"
    lang[f"tag.item.{MOD}.concordance_instruments"] = "Concordance Instruments"
    lang[f"tag.item.{MOD}.concordance.luminous_matter"] = "Luminous Matter"
    lang[f"config.jade.plugin_{MOD}.lampwright_bench"] = "Lampwright's Bench"
    lang[f"config.jade.plugin_{MOD}.lumen_sconce"] = "Lumen Sconce"
    # JEI's bench category (client/compat/JugcraftJeiPlugin): what a working leaves in the lantern.
    lang[f"jei.{MOD}.concordance.craft"] = "Kindled with %s Radiance; needs First Light"
    lang[f"jei.{MOD}.concordance.infuse"] = "+%s Radiance; needs First Light"
    lang[f"key.{MOD}.concordance_config"] = "Concordance settings"


def bench_model():
    """The Lampwright's Bench: a small oak desk with brass rails, a specimen dish and a hinged lens on a brass arm,
    real-life desk sized (one block, a little lower than a crafting table). Faces are drawn by
    tools/concordance_art.py. The lens is emissive while the bench works."""
    def box(f, t, tex, emissive=False):
        element = {"from": f, "to": t, "faces": {d: {"texture": f"#{tex}"} for d in
                                                ("north", "east", "south", "west", "up", "down")}}
        if emissive:
            element["light_emission"] = 15
        return element
    elements = [
        box([0, 12, 0], [16, 14, 16], "top"),            # desk top
        box([1, 0, 1], [3, 12, 3], "wood"),              # legs
        box([13, 0, 1], [15, 12, 3], "wood"),
        box([1, 0, 13], [3, 12, 15], "wood"),
        box([13, 0, 13], [15, 12, 15], "wood"),
        box([3, 9, 1.5], [13, 11, 2.5], "brass"),        # apron rails
        box([3, 9, 13.5], [13, 11, 14.5], "brass"),
        box([3, 14, 9], [8, 15, 14], "brass"),           # specimen dish
        box([11, 14, 3], [13, 22, 5], "brass"),          # lens arm post
        box([6.5, 21, 3.5], [12.5, 22, 4.5], "brass"),   # lens arm
    ]
    return elements


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    rituals.write_all(write, assets, data, lang, condition, self_drop, assets.parents[1] / "resourcepacks")
    alchemy.write_all(write, assets, data, lang, condition, self_drop)
    ecology.write_all(write, assets, data, lang, condition, self_drop, assets.parents[1] / "resourcepacks")
    celestial.write_all(write, assets, data, lang, condition, self_drop)
    crimson.write_all(write, assets, data, lang, condition, self_drop)
    workers.write_all(write, assets, data, lang, condition, self_drop)
    logistics.write_all(write, assets, data, lang, condition, self_drop)
    artifice.write_all(write, assets, data, lang, condition, self_drop)
    relics.write_all(write, assets, data, lang, condition, self_drop)
    equivalence.write_all(write, assets, data, lang, condition, self_drop)
    hexes.write_all(write, assets, data, lang, condition, self_drop)
    conclave.write_all(write, assets, data, lang, condition, self_drop)
    progression.write_all(write, data, lang)
    spire.write_all(write, assets, data, lang, condition, self_drop)
    ember.write_all(write, assets, data, lang, condition, self_drop)
    trinkets.write_all(write, assets, data, lang, condition, self_drop)
    journal.lang_entries(lang)
    signs.lang_entries(lang)
    # Items.
    write(assets / "models" / "item" / "initiate_wand.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": rid("item/initiate_wand")}})
    write(assets / "items" / "initiate_wand.json", {"model": {"type": "minecraft:model", "model": rid("item/initiate_wand")}})
    for model in ("kindled_lantern", "kindled_lantern_lit"):
        write(assets / "models" / "item" / f"{model}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{model}")}})
    write(assets / "items" / "kindled_lantern.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component", "component": rid("lantern_lit"),
        "on_true": {"type": "minecraft:model", "model": rid("item/kindled_lantern_lit")},
        "on_false": {"type": "minecraft:model", "model": rid("item/kindled_lantern")}}})
    # The codex book's model (Modonomicon reads book.json "model" as an item model definition).
    write(assets / "models" / "item" / f"{BOOK}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{BOOK}")}})
    write(assets / "items" / f"{BOOK}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{BOOK}")}})
    # The bench.
    textures = {"particle": rid("block/lampwright_bench_wood"), "top": rid("block/lampwright_bench_top"),
                "wood": rid("block/lampwright_bench_wood"), "brass": rid("block/lampwright_bench_brass"),
                "lens": rid("block/lampwright_bench_lens")}
    elements = bench_model()
    lens = {"from": [6, 18, 2], "to": [10, 22, 3], "faces": {d: {"texture": "#lens"} for d in
                                                              ("north", "east", "south", "west", "up", "down")}}
    lit_lens = dict(lens, light_emission=15)
    write(assets / "models" / "block" / "lampwright_bench.json",
          {"parent": "minecraft:block/block", "textures": textures, "elements": elements + [lens]})
    write(assets / "models" / "block" / "lampwright_bench_working.json",
          {"parent": "minecraft:block/block", "textures": textures, "elements": elements + [lit_lens]})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for working in ("false", "true"):
            model = rid("block/lampwright_bench_working" if working == "true" else "block/lampwright_bench")
            variants[f"facing={facing},working={working}"] = {"model": model, **({"y": y} if y else {})}
    write(assets / "blockstates" / "lampwright_bench.json", {"variants": variants})
    write(assets / "items" / "lampwright_bench.json",
          {"model": {"type": "minecraft:model", "model": rid("block/lampwright_bench")}})
    write(data / "loot_table" / "blocks" / "lampwright_bench.json", self_drop("lampwright_bench"))
    # Research Notes: a blank and a written sheet.
    for model in ("research_notes", "research_notes_written"):
        write(assets / "models" / "item" / f"{model}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{model}")}})
    write(assets / "items" / "research_notes.json", {"model": {
        "type": "minecraft:condition", "property": "minecraft:has_component", "component": rid("research_notes"),
        "on_true": {"type": "minecraft:model", "model": rid("item/research_notes_written")},
        "on_false": {"type": "minecraft:model", "model": rid("item/research_notes")}}})
    # The Lumen Sconce: a brass foot, stem and cup holding a lens that glows while it burns.
    sconce_textures = {"particle": rid("block/lampwright_bench_brass"), "brass": rid("block/lampwright_bench_brass"),
                       "lens": rid("block/lampwright_bench_lens")}

    def part(f, t, tex, emissive=False):
        element = {"from": f, "to": t, "faces": {d: {"texture": f"#{tex}"} for d in
                                                ("north", "east", "south", "west", "up", "down")}}
        if emissive:
            element["light_emission"] = 15
        return element
    for lit in (False, True):
        elements = [part([4, 0, 4], [12, 1, 12], "brass"), part([7, 1, 7], [9, 7, 9], "brass"),
                    part([5, 7, 5], [11, 8, 11], "brass"), part([6, 8, 6], [10, 12, 10], "lens", emissive=lit),
                    part([7.5, 12, 7.5], [8.5, 13, 8.5], "brass")]
        name = "lumen_sconce_lit" if lit else "lumen_sconce"
        write(assets / "models" / "block" / f"{name}.json",
              {"parent": "minecraft:block/block", "textures": sconce_textures, "elements": elements})
    write(assets / "blockstates" / "lumen_sconce.json",
          {"variants": {"lit=false": {"model": rid("block/lumen_sconce")}, "lit=true": {"model": rid("block/lumen_sconce_lit")}}})
    write(assets / "items" / "lumen_sconce.json", {"model": {"type": "minecraft:model", "model": rid("block/lumen_sconce")}})
    # Broken, the sconce keeps its Radiance on the item (LumenSconceBlockEntity.collectImplicitComponents).
    sconce_loot = self_drop("lumen_sconce")
    sconce_loot["pools"][0]["entries"][0]["modifier"] = [
        {"type": "minecraft:copy_components", "source": "block_entity", "include": [rid("radiance")]}]
    write(data / "loot_table" / "blocks" / "lumen_sconce.json", sconce_loot)
    # The Kindled mote: no model is drawn (it renders nothing); particles show it.
    write(assets / "models" / "block" / "lumen_mote.json", {"textures": {"particle": rid("block/lampwright_bench_lens")}})
    write(assets / "blockstates" / "lumen_mote.json",
          {"variants": {"": {"model": rid("block/lumen_mote")}}})
    # Recipes (vanilla crafting; the bench's own workings are Concordance data below).
    write(data / "recipe" / "initiate_wand.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["  A", " S ", "C  "],
        "key": {"A": f"#{LUMINOUS_MATTER_TAG}", "S": "minecraft:stick", "C": "minecraft:copper_ingot"},
        "result": {"id": rid("initiate_wand"), "count": 1}})
    write(data / "recipe" / "lampwright_bench.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["CAC", "PPP", "S S"],
        "key": {"C": "minecraft:copper_ingot", "A": f"#{LUMINOUS_MATTER_TAG}", "P": "#minecraft:planks",
                "S": "minecraft:stick"},
        "result": {"id": rid("lampwright_bench"), "count": 1}})
    write(data / "recipe" / "research_notes.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:paper", "minecraft:ink_sac", "minecraft:feather"],
        "result": {"id": rid("research_notes"), "count": 2}})
    write(data / "recipe" / "lumen_sconce.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": [" A ", " C ", "CCC"],
        "key": {"A": f"#{LUMINOUS_MATTER_TAG}", "C": "minecraft:copper_ingot"},
        "result": {"id": rid("lumen_sconce"), "count": 1}})
    write(data / "recipe" / f"{BOOK}.json", {
        "fabric:load_conditions": condition(FEATURE), "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["minecraft:book", f"#{LUMINOUS_MATTER_TAG}"],
        "result": {"id": "modonomicon:modonomicon", "count": 1, "components": {"modonomicon:book_id": rid(BOOK)}}})


def write_data(write, res):
    """Data outside the generator's usual folders, all under data/jugcraft/ (cleaned by generate_material_data.main
    through GENERATED_DIRS) and the client assets that only Spell Engine, PAL and LambDynamicLights read."""
    data = res / "data" / MOD
    assets = res / "assets" / MOD
    for key, info in RESEARCH.items():
        write(data / "concordance" / "research" / f"{key}.json", research_json(key, info))
    for key, info in INVOCATIONS.items():
        write(data / "concordance" / "invocation" / f"{key}.json", invocation_json(key, info))
        write(data / "spell" / f"{key}.json", spell_json(key))
    for key, info in WORKINGS.items():
        write(data / "concordance" / "working" / f"{key}.json", dict({"schema": SCHEMA}, **info))
    for key, info in CONVERSIONS.items():
        write(data / "concordance" / "conversion" / f"{key}.json", dict({"schema": SCHEMA}, **info))
    for key, info in COMPONENTS.items():
        write(data / "concordance" / "component" / f"{key}.json", component_json(info))
    for key, info in INSTRUMENT_LIMITS.items():
        write(data / "concordance" / "instrument" / f"{key}.json", dict({"schema": SCHEMA}, **info))
    write(data / "spell" / "composed.json", composed_spell_json())
    for item in spell_opt_outs():
        write(data / "spell_assignments" / f"{item}.json", {"access": "NONE", "access_param": ""})
    # Instruments resolve casts for the Concordance spell tag only; which of its spells a player has is the
    # Concordance's own container source (Java: ConcordanceSpells), so only understood invocations show.
    for item in INSTRUMENTS:
        write(data / "spell_assignments" / f"{item}.json", {"access": "TAG", "access_param": SPELL_TAG})
    lang = {}
    for path, content in codex_files(lang).items():
        write(data / path, content)
    for name, clip in player_animations().items():
        write(assets / "player_animations" / f"{name}.json", clip)
    rituals.write_data(write, data, assets)
    alchemy.write_data(write, data)
    ecology.write_data(write, data)
    celestial.write_data(write, data)
    crimson.write_data(write, data)
    workers.write_data(write, data)
    logistics.write_data(write, data)
    artifice.write_data(write, data)
    relics.write_data(write, data)
    equivalence.write_data(write, data)
    hexes.write_data(write, data)
    ember.write_data(write, data)
    trinkets.write_data(write, data)
    conclave.write_data(write, data)
    progression.write_data(write, data)
    spire.write_data(write, data)
    # LambDynamicLights (optional, client): a lit Kindled Lantern glows in hand. Without the mod nothing reads this.
    write(assets / "dynamiclights" / "item" / "kindled_lantern.json",
          {"match": {"items": rid("kindled_lantern"), "components": {rid("lantern_lit"): {}}},
           "luminance": TRAIL_LIGHT})
    return lang


def tags(tags):
    for ref in SPECIMENS:
        tags.add("item", SPECIMEN_TAG, ref)
    for tag in SPECIMEN_TAGS:
        tags.add("item", ALL_SPECIMENS_TAG, f"#{tag}")
    rituals.tags(tags)
    alchemy.tags(tags)
    ecology.tags(tags)
    celestial.tags(tags)
    crimson.tags(tags)
    workers.tags(tags)
    logistics.tags(tags)
    artifice.tags(tags)
    relics.tags(tags)
    equivalence.tags(tags)
    hexes.tags(tags)
    conclave.tags(tags)
    spire.tags(tags)
    ember.tags(tags)
    trinkets.tags(tags)
    for item in INSTRUMENTS:
        tags.add("item", INSTRUMENT_TAG, rid(item))
    for item in LUMINOUS_MATTER:
        tags.add("item", LUMINOUS_MATTER_TAG, item)
    tags.add("block", "minecraft:mineable/axe", rid("lampwright_bench"))
    tags.add("block", "minecraft:mineable/pickaxe", rid("lumen_sconce"))
    for key in INVOCATIONS:
        tags.add("spell", SPELL_TAG, rid(key))
    tags.add("spell", SPELL_TAG, rid("composed"))
    # The shared effect system's tolerances and block lists (Java: ConcordanceEffects).
    for entity in EFFECT_IMMUNE:
        tags.add("entity_type", f"{MOD}:concordance/immune", entity)
    for entity in EFFECT_RESISTANT:
        tags.add("entity_type", f"{MOD}:concordance/resistant", entity)
    for block in INTERACTABLE:
        tags.add("block", f"{MOD}:concordance/interactable", block)
    for block in HARVESTABLE:
        tags.add("block", f"{MOD}:concordance/harvestable", block)


def recipe_stations():
    """Concordance stations after the bench, for JEI: the Circle Anchor's rituals that make an item."""
    return [rituals.recipe_station()]


def recipe_view():
    """The bench's workings for JEI (client/compat/JugcraftJeiPlugin): public crafts, not secret research."""
    rows = [{"kind": "craft", "in": [[WORKINGS["kindle_lantern"]["work"], 1], [WORKINGS["kindle_lantern"]["specimen"], 1]],
             "out": [WORKINGS["kindle_lantern"]["result"], 1], "radiance": LANTERN_START}]
    for ref, value in SPECIMENS.items():
        rows.append({"kind": "infuse", "in": [[rid("kindled_lantern"), 1], [ref, 1]], "out": [rid("kindled_lantern"), 1],
                     "radiance": value})
    return {"block": rid("lampwright_bench"), "recipes": rows}
