"""Roadmap step 15: celestial cycles and attunement (docs/features/arcane-concordance-celestial.md).

The Starwatchers' practice. The sky's calendar is the overworld's clock, the clock vanilla's sun and moon follow: day d
is floor(time / 24000), the moon is full on days where d mod 8 is 0, and a pattern comes round every `period` days on
the days where d mod period is `offset`, between two ticks of the day. Everything is worked out from that clock on the
server, so every client and every save agrees; the rendered sky, a shader's moon or a client's settings never decide.

An Orrery Observatory under the open sky gathers each pattern's Astral Resonance once per occurrence for its keeper:
the occurrence's number (counted from day zero) is the reward's identity, and a claim must be for a later occurrence
than the last and at least half a period of game time after it (game time only runs forward with the world), so moving
the clock back or forward never pays twice. An Astrolabe carries resonance and attunes its holder to a pattern that is
up, for the pattern's effect while it stays up (at most eight minutes); a master may recall a pattern they observed
when nothing is up, dearer and shorter, once per occurrence by the same ledger.

The patterns are data (data/jugcraft/concordance/pattern, read by Java's concordance/celestial). Seasonal patterns keep
to the established calendars: Jugcraft's seasons (the server's date) and the Halloween event's Harvest Moon.

tools/concordance.py merges these tables into its own. Numbers the Java repeats are checked by tools/check_mod_data.py.
"""

MOD = "jugcraft"


def rid(path):
    return f"{MOD}:{path}"


# ------------------------------------------------------------------------------------------------------- numbers

# The calendar (Java: celestial/Calendar.java).
DAY = 24000
RECALL_TICKS = 2400  # a recalled attunement lasts two minutes
MAX_FORECAST_DAYS = 32
MAX_ATTUNEMENT_TICKS = 9600  # eight minutes: vanilla's longest potion
# Night on the overworld clock, as the Harvest Moon has it (Java: agriculture/HarvestMoon.java).
DUSK = 13000
DAWN = 23000
# Limits a pattern's data must keep (Java: celestial/CelestialParser.java).
MAX_RESONANCE = 16
MAX_COST = 32
MAX_PERIOD = 64
MAX_AMPLIFIER = 1
SEASONS = ["winter", "spring", "summer", "autumn", "harvest_moon"]  # Java: celestial/Pattern.SEASONS
# The observatory (Java: sky/ObservatoryBlockEntity.java) and the astrolabe (sky/AstrolabeItem.java).
OBSERVATORY_CAPACITY = 32
GATHER_TICKS = 100
ASTROLABE_CAPACITY = 32
# Attunements (Java: sky/Sky.java): a pulse every PULSE_TICKS gives the effect for EFFECT_TICKS.
PULSE_TICKS = 100
EFFECT_TICKS = 140
FORECAST_DAYS = 8
# The status effects a pattern may give: helpful, with a vanilla potion or a vanilla source of their own.
ATTUNEMENT_EFFECTS = {
    "minecraft:night_vision": "Night Vision", "minecraft:water_breathing": "Water Breathing",
    "minecraft:haste": "Haste", "minecraft:speed": "Speed", "minecraft:fire_resistance": "Fire Resistance",
    "minecraft:luck": "Luck", "minecraft:hero_of_the_village": "Hero of the Village",
    "minecraft:jump_boost": "Jump Boost", "minecraft:slow_falling": "Slow Falling",
}

CELESTIAL_SPECIMEN_TAG = f"{MOD}:celestial_specimens"
CELESTIAL_SPECIMENS = {
    "minecraft:spyglass": "Spyglass", "minecraft:clock": "Clock", "minecraft:compass": "Compass",
    "minecraft:phantom_membrane": "Phantom Membrane", "minecraft:glow_ink_sac": "Glow Ink Sac",
}
OBSERVATION = rid("observation")  # the practice an observation records (Java: Sky.ACTIVITY)
MASTERY_PATTERNS = 3  # distinct patterns observed to master Celestial Attunement

# ------------------------------------------------------------------------------------------------- research

RESEARCH = {
    "celestial_attunement": {
        "name": "Celestial Attunement",
        "principle": "radiance",
        "tradition": "starwatchers",
        "stage": "practitioner",
        "icon": rid("astrolabe"),
        "requires": [{"research": rid("first_light"), "state": "understood"}],
        "states": {
            "encountered": [{"type": "examine", "specimens": f"#{CELESTIAL_SPECIMEN_TAG}"}],
            "observed": [{"type": "examine", "specimens": f"#{CELESTIAL_SPECIMEN_TAG}", "distinct": 3}],
            "understood": [
                {"type": "study", "station": rid("lampwright_bench"), "specimens": f"#{CELESTIAL_SPECIMEN_TAG}"},
                {"type": "notes"},
            ],
            # Mastery is practice: patterns seen through an observatory with your own eyes.
            "mastered": [{"type": "practice", "activity": OBSERVATION, "distinct": MASTERY_PATTERNS}],
        },
        "unlocks": {},
        "advancements": {
            "encountered": ("Stargazer", "Examine an instrument of the sky", "task", "minecraft:spyglass"),
            "observed": ("Almanac", "Examine three different instruments of the sky", "task", "minecraft:clock"),
            "understood": ("Celestial Attunement", "Understand Celestial Attunement: study an instrument of the sky "
                           "at a Lampwright's Bench", "goal", rid("observatory")),
            "mastered": ("Starwatcher", "Observe three different celestial patterns through an observatory",
                         "challenge", rid("astrolabe")),
        },
        "locked": {"encountered": "Examine an instrument of the sky first",
                   "observed": "Examine three different instruments of the sky first",
                   "understood": "Understand Celestial Attunement first",
                   "mastered": "Master Celestial Attunement first"},
    },
}

# ------------------------------------------------------------------------------------------------- patterns

# "period" and "offset" pick the days (day mod period == offset), "from" and "to" the ticks of the day (0 is dawn,
# 13000 dusk, 18000 midnight). "resonance" is what one occurrence pays an observatory; "cost" what attuning takes from
# an astrolabe while it is up, "recall" what recalling it takes when it is not (always dearer).
PATTERNS = {
    # Vanilla's full moon (day mod 8 == 0): the brightest night.
    "full_moon": {"name": "The Full Moon", "principle": "radiance", "period": 8, "offset": 0, "from": DUSK, "to": DAWN,
                  "clear_sky": True, "resonance": 4, "effect": "minecraft:night_vision", "amplifier": 0, "cost": 4,
                  "recall": 12, "attunement": "Moonsight",
                  "summary": "Moonsight: you see in the dark while it is up."},
    # Vanilla's new moon (day mod 8 == 4): the tides run highest; a dark sky is dark in any weather.
    "new_moon": {"name": "The New Moon", "principle": "tide", "period": 8, "offset": 4, "from": DUSK, "to": DAWN,
                 "clear_sky": False, "resonance": 4, "effect": "minecraft:water_breathing", "amplifier": 0, "cost": 4,
                 "recall": 12, "attunement": "Spring Tide",
                 "summary": "Spring Tide: you breathe under water while it is up."},
    # An evening star every third day, low at dusk.
    "lantern_star": {"name": "The Lantern Star", "principle": "radiance", "period": 3, "offset": 1, "from": 12000,
                     "to": 14000, "clear_sky": True, "resonance": 2, "effect": "minecraft:haste", "amplifier": 0,
                     "cost": 2, "recall": 6, "attunement": "Lantern Hands",
                     "summary": "Lantern Hands: you dig and work faster while it is up."},
    # A comet every nineteenth night: rare and rich.
    "echo_comet": {"name": "The Echo Comet", "principle": "echo", "period": 19, "offset": 7, "from": 14000, "to": 22000,
                   "clear_sky": True, "resonance": 8, "effect": "minecraft:speed", "amplifier": 0, "cost": 6,
                   "recall": 16, "attunement": "Comet's Wake",
                   "summary": "Comet's Wake: you run faster while it is up."},
    # Winter's crown of stars, every other night of the season.
    "winter_crown": {"name": "The Winter Crown", "principle": "rime", "period": 2, "offset": 1, "from": 15000,
                     "to": 21000, "clear_sky": True, "resonance": 3, "effect": "minecraft:fire_resistance",
                     "amplifier": 0, "cost": 3, "recall": 9, "season": "winter", "attunement": "Frostmantle",
                     "summary": "Frostmantle: fire does not burn you while it is up."},
    # The Halloween event's Harvest Moon, every night it rises (agriculture/HarvestMoon.java decides when).
    "harvest_moon": {"name": "The Harvest Moon", "principle": "verdance", "period": 1, "offset": 0, "from": DUSK,
                     "to": DAWN, "clear_sky": False, "resonance": 6, "effect": "minecraft:hero_of_the_village",
                     "amplifier": 0, "cost": 4, "recall": 12, "season": "harvest_moon", "attunement": "Feast Favour",
                     "summary": "Feast Favour: villagers trade with you as with a hero while it is up."},
}

# ------------------------------------------------------------------------------------------------- things

ITEMS = {
    "astrolabe": {"name": "Astrolabe",
                  "tooltip": "Draw resonance from an observatory; use it under a risen pattern to attune to it."},
}
BLOCKS = {"observatory": {"name": "Orrery Observatory"}}
DEVICE_TOOLTIPS = {
    "observatory": "Under the open sky it gathers Astral Resonance from each celestial pattern once. Use it with an "
                   "empty hand for the forecast.",
}

# ------------------------------------------------------------------------------------------------- text

MOON_PHASES = ["full moon", "waning gibbous", "last quarter", "waning crescent", "new moon", "waxing crescent",
               "first quarter", "waxing gibbous"]
SEASON_NAMES = {"winter": "winter", "spring": "spring", "summer": "summer", "autumn": "autumn",
                "harvest_moon": "the Harvest Moon", "off": "no seasons"}

MESSAGES = {
    "sky.unknown": "You have not understood Celestial Attunement (the codex says how)",
    "sky.today": "Day %s, %s: %s, %s",
    "sky.line": "%s: %s, %s%s",
    "sky.up": "up now, until %s",
    "sky.tonight": "rises today at %s",
    "sky.rises": "rises in %s days at %s",
    "sky.clear": "needs a clear sky",
    "sky.any": "any weather",
    "sky.only": ", only in %s",
    "sky.none": "Nothing comes round in the next %s days",
    "sky.observatory": "Orrery Observatory: %s / %s resonance, %s",
    "sky.aligned": "Aligned: it gathers for %s",
    "sky.observed": "Observed: %s",
    "sky.astrolabe_full": "The astrolabe holds all it can",
    "sky.nothing_to_draw": "The observatory holds no resonance",
    "sky.not_yours": "Only its keeper (or their party) may draw from it",
    "sky.drawn": "Drew %s resonance: %s / %s",
    "sky.too_little": "The astrolabe needs %s resonance",
    "sky.attuned": "Attuned to %s",
    "sky.nothing_up": "Nothing is up where you can see it (a master may recall a pattern)",
    "sky.nothing_observed": "You have observed no pattern to recall",
    "sky.recalled_already": "You have recalled %s since it last rose",
    "sky.recalled": "Recalled %s",
}
for _phase, _name in enumerate(MOON_PHASES):
    MESSAGES[f"sky.moon.{_phase}"] = _name
for _season, _name in SEASON_NAMES.items():
    MESSAGES[f"sky.season.{_season}"] = _name

# The observatory's statuses (compose.jugcraft.sky.status.*; Java: ObservatoryBlockEntity.gather).
STATUSES = {
    "waiting": "nothing is up",
    "disabled": "switched off",
    "unaligned": "unaligned: a Starwatcher's touch aligns it",
    "gathering": "gathering",
    "gathered": "already gathered from what is up",
    "too_soon": "too soon since it last gathered from this pattern",
    "full": "full",
    "clouded": "clouded over",
    "no_sky": "something hides the sky",
    "elsewhere": "only the Overworld's sky",
}
TEXT = {f"sky.status.{key}": text for key, text in STATUSES.items()}

TOOLTIPS = {
    "celestial_specimen": "Instrument of the sky: sneak and use to examine it",
    "astrolabe.charge": "Resonance: %s / %s",
    "jade.resonance": "Resonance: %s / %s",
    "jade.visible": "In view: %s",
}


def lang_entries(lang):
    lang[f"tag.item.{CELESTIAL_SPECIMEN_TAG.replace(':', '.')}"] = "Instruments of the Sky"
    for key, text in TEXT.items():
        lang[f"compose.{MOD}.{key}"] = text
    for key, info in PATTERNS.items():
        lang[f"pattern.{MOD}.{key}"] = info["name"]
    lang[f"config.jade.plugin_{MOD}.observatory"] = BLOCKS["observatory"]["name"]
    for key, text in DEVICE_TOOLTIPS.items():
        lang[f"tooltip.{MOD}.{key}"] = text


# ------------------------------------------------------------------------------------------------- codex

def clock(tick):
    """A tick of the day as a clock reads it (Java: Sky.clock): tick 0 is 06:00."""
    minutes = (tick * 60 // 1000 + 6 * 60) % (24 * 60)
    return f"{minutes // 60:02d}:{minutes % 60:02d}"


def when_text(key):
    info = PATTERNS[key]
    if info["period"] == 1:
        days = "every night"
    elif info["period"] == 8 and info["offset"] == 0:
        days = "on the full moon's night"
    elif info["period"] == 8 and info["offset"] == 4:
        days = "on the new moon's night"
    else:
        days = f"every {info['period']} days (days that leave {info['offset']} over {info['period']})"
    season = f", in {SEASON_NAMES[info['season']]}" if info.get("season") else ""
    weather = "a clear sky" if info["clear_sky"] else "any weather"
    return f"{days}{season}, {clock(info['from'])} to {clock(info['to'])}, {weather}"


def _pattern_table():
    rows = []
    for key, info in PATTERNS.items():
        rows.append(f"- **{info['name']}**: {when_text(key)}. Pays {info['resonance']} resonance; "
                    f"{info['summary']} Attuning costs {info['cost']}, recalling {info['recall']}.")
    return "\\\n".join(rows)


def codex():
    art = "celestial_attunement"
    specimens = "\\\n".join(f"- {name}" for name in CELESTIAL_SPECIMENS.values())
    return {
        ("sky", "celestial_attunement"): {
            "name": "Celestial Attunement", "x": 0, "y": 0, "icon": rid("astrolabe"), "condition": None,
            "description": "The sky: research",
            "pages": [
                ("text", "Celestial Attunement",
                 "Once you understand First Light, look closely at the instruments that measure the sky. **Sneak "
                 "and use** one of these to examine it:\\\n" + specimens),
                ("text", "Observed", "Study one at a **Lampwright's Bench**, or read someone's **Research Notes**.",
                 (art, "observed")),
                ("text", "Understood",
                 f"Your touch now aligns an Orrery Observatory, and an Astrolabe answers you. To **master** "
                 f"Celestial Attunement, observe {MASTERY_PATTERNS} different patterns through an observatory "
                 f"(use it with an empty hand while one is up).", (art, "understood")),
                ("text", "Mastered", "You can recall a pattern you have seen when the sky is empty.",
                 (art, "mastered")),
            ],
        },
        ("sky", "calendar"): {
            "name": "The Calendar", "x": 2, "y": 0, "icon": "minecraft:clock", "condition": (art, "encountered"),
            "description": "What comes round, and when",
            "pages": [
                ("text", "The World's Clock",
                 "The sky keeps the world's own clock, the one the sun and moon follow: a day is 20 minutes, night "
                 "runs from 19:00 to 05:00, and the moon goes from full to new and back every 8 days. Every player "
                 "sees the same sky, and so does the world after a restart. What your screen draws (a shader's moon, "
                 "your render settings) never changes what is up."),
                ("text", "The Patterns", _pattern_table()),
                ("text", "The Forecast",
                 f"Use an Orrery Observatory with an empty hand, or ask with **/jugcraft concordance sky**, to read "
                 f"the forecast: today's day, time, moon and season, and every pattern up now or rising in the next "
                 f"{FORECAST_DAYS} days, with when it rises, the weather it needs and the time of year it keeps to."),
                ("text", "Seasons",
                 "Some patterns keep to a time of year. The Winter Crown comes round only in Jugcraft's winter (the "
                 "server's date; with seasons off, all year). The Harvest Moon pattern is the Halloween event's own "
                 "Harvest Moon: it rises on the nights the event says, and never outside them."),
            ],
        },
        ("sky", "observatory"): {
            "name": "Orrery Observatory", "x": 4, "y": 0, "icon": rid("observatory"), "condition": (art, "observed"),
            "description": "Gathering resonance from the sky",
            "pages": [
                ("crafting_recipe", "Orrery Observatory",
                 f"Set it under the open sky in the Overworld. Placed by a Starwatcher it is aligned to them; "
                 f"otherwise a Starwatcher's empty hand aligns it. Every {GATHER_TICKS // 20} seconds it looks up: "
                 f"from each pattern up and in view it gathers that pattern's resonance, up to "
                 f"{OBSERVATORY_CAPACITY}.", rid("observatory")),
                ("text", "Once Each Time",
                 "Each time a pattern comes round it pays its keeper once, however many observatories they keep. "
                 "Turning the world's clock back brings back only nights that have paid; turning it forward cannot "
                 "pay sooner than half the pattern's period of real play. Its keeper's party may draw its resonance "
                 "into an astrolabe (use the astrolabe on it)."),
                ("text", "Why It Waits",
                 "It says why it is idle: nothing up, unaligned, clouded over (for a pattern that needs a clear "
                 "sky), something hiding the sky, not in the Overworld, full, already gathered, or too soon."),
            ],
        },
        ("sky", "attunement"): {
            "name": "Attunement", "x": 2, "y": 2, "icon": rid("astrolabe"), "condition": (art, "understood"),
            "description": "The astrolabe and what it gives",
            "pages": [
                ("crafting_recipe", "Astrolabe",
                 f"It holds up to {ASTROLABE_CAPACITY} resonance. Use it where you can see a risen pattern: it spends "
                 f"the pattern's cost and attunes you to it, and its effect is yours while the pattern stays up (at "
                 f"most {MAX_ATTUNEMENT_TICKS // 1200} minutes; use it again to renew). Death ends an attunement.",
                 rid("astrolabe")),
                ("text", "Recall",
                 f"A master may attune when nothing is up, by **recalling** the pattern they last observed: it costs "
                 f"the pattern's recall price and lasts {RECALL_TICKS // 1200} minutes, once each time the pattern "
                 f"comes round. Stored resonance is the alternative to waiting, never a way round the sky's count.",
                 (art, "mastered")),
            ],
        },
    }


CATEGORY = {"sky": {"name": "Sky", "icon": rid("astrolabe"), "sort": 7,
                    "description": "The Starwatchers' practice: celestial patterns, observatories and attunement"}}


# ------------------------------------------------------------------------------------------------- data and assets

def pattern_json(key):
    info = PATTERNS[key]
    out = {"schema": 1, "principle": info["principle"], "period": info["period"], "offset": info["offset"],
           "from": info["from"], "to": info["to"], "clear_sky": info["clear_sky"], "resonance": info["resonance"],
           "attunement": {"effect": info["effect"], "amplifier": info["amplifier"], "cost": info["cost"],
                          "recall": info["recall"]}}
    if info.get("season"):
        out["season"] = info["season"]
    return out


def tags(tags):
    for ref in CELESTIAL_SPECIMENS:
        tags.add("item", CELESTIAL_SPECIMEN_TAG, ref)
    tags.add("block", "minecraft:mineable/pickaxe", rid("observatory"))


def write_all(write, assets, data, lang, condition, self_drop):
    lang_entries(lang)
    # The observatory: GeckoLib draws it; its block model is only the breaking particles.
    write(assets / "geckolib" / "models" / "block" / "observatory.geo.json", observatory_geo())
    write(assets / "geckolib" / "animations" / "block" / "observatory.animation.json", observatory_animations())
    write(assets / "models" / "block" / "observatory.json", {"textures": {"particle": rid("block/observatory")}})
    write(assets / "blockstates" / "observatory.json", {"variants": {"": {"model": rid("block/observatory")}}})
    write(data / "loot_table" / "blocks" / "observatory.json", self_drop("observatory"))
    for item in ("observatory", "astrolabe"):
        write(assets / "models" / "item" / f"{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": rid(f"item/{item}")}})
        write(assets / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": rid(f"item/{item}")}})
    # LambDynamicLights (optional, client): an astrolabe holding resonance glows faintly in hand.
    write(assets / "dynamiclights" / "item" / "astrolabe.json",
          {"match": {"items": rid("astrolabe"), "components": {rid("resonant"): {}}}, "luminance": 6})
    feature = "concordance"
    recipes = {
        "observatory": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" S ", "GKG", "CCC"],
                        "key": {"S": "minecraft:spyglass", "G": "minecraft:gold_ingot", "K": "minecraft:clock",
                                "C": "minecraft:copper_ingot"},
                        "result": {"id": rid("observatory"), "count": 1}},
        "astrolabe": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" G ", "GCG", " A "],
                      "key": {"G": "minecraft:gold_ingot", "C": "minecraft:compass", "A": "minecraft:amethyst_shard"},
                      "result": {"id": rid("astrolabe"), "count": 1}},
    }
    for name, recipe in recipes.items():
        write(data / "recipe" / f"{name}.json", dict({"fabric:load_conditions": condition(feature)}, **recipe))


def write_data(write, data):
    for key in PATTERNS:
        write(data / "concordance" / "pattern" / f"{key}.json", pattern_json(key))


# ------------------------------------------------------------------------------------------------- GeckoLib body

def _cube(uv, name, origin, size, rotation=None, pivot=None):
    u, v = uv[name]
    cube = {"origin": origin, "size": size, "uv": [u, v]}
    if rotation:
        cube["rotation"] = rotation
        cube["pivot"] = pivot
    return cube


# Box-UV origins in the 64x64 sheet; a cube of size (w, h, d) uses (2(w + d)) x (d + h) from there
# (tools/concordance_celestial_art.py paints the same regions).
OBSERVATORY_UV = {"plinth": (0, 0), "pillar": (0, 16), "yoke": (16, 16), "arm": (40, 16), "tube": (0, 32),
                  "eyepiece": (40, 32), "lens": (40, 40), "ring": (0, 48)}
OBSERVATORY_SIZES = {"plinth": (12, 2, 12), "pillar": (4, 5, 4), "yoke": (8, 2, 4), "arm": (1, 4, 2),
                     "tube": (4, 4, 12), "eyepiece": (2, 2, 3), "lens": (4, 4, 1), "ring": (6, 1, 6)}


def observatory_geo():
    """The Orrery Observatory: a brass telescope on a yoke that turns on a pillar over a round plinth."""
    uv = OBSERVATORY_UV
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.observatory", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 1, 0]},
        "bones": [
            {"name": "plinth", "pivot": [0, 0, 0], "cubes": [_cube(uv, "plinth", [-6, 0, -6], [12, 2, 12]),
                                                             _cube(uv, "ring", [-3, 2, -3], [6, 1, 6])]},
            {"name": "pillar", "parent": "plinth", "pivot": [0, 3, 0],
             "cubes": [_cube(uv, "pillar", [-2, 3, -2], [4, 5, 4])]},
            {"name": "mount", "parent": "pillar", "pivot": [0, 8, 0], "cubes": [
                _cube(uv, "yoke", [-4, 8, -2], [8, 2, 4]), _cube(uv, "arm", [-4, 10, -1], [1, 4, 2]),
                _cube(uv, "arm", [3, 10, -1], [1, 4, 2])]},
            {"name": "tube", "parent": "mount", "pivot": [0, 12, 0], "rotation": [-20, 0, 0], "cubes": [
                _cube(uv, "tube", [-2, 10, -6], [4, 4, 12]), _cube(uv, "lens", [-2, 10, -7], [4, 4, 1]),
                _cube(uv, "eyepiece", [-1, 11, 6], [2, 2, 3])]},
        ]}]}


def observatory_animations():
    """Idle: the yoke sweeps slowly round, the tube low. Tracking: the tube lifts to the sky and follows it."""
    return {"format_version": "1.8.0", "animations": {
        "animation.observatory.idle": {"loop": True, "animation_length": 20.0, "bones": {
            "mount": {"rotation": {"0.0": [0, 0, 0], "10.0": [0, 180, 0], "20.0": [0, 360, 0]}},
            "tube": {"rotation": {"0.0": [0, 0, 0]}}}},
        "animation.observatory.tracking": {"loop": True, "animation_length": 8.0, "bones": {
            "mount": {"rotation": {"0.0": [0, -15, 0], "4.0": [0, 15, 0], "8.0": [0, -15, 0]}},
            "tube": {"rotation": {"0.0": [-30, 0, 0], "4.0": [-38, 0, 0], "8.0": [-30, 0, 0]}}}},
    }}


GEO = {"observatory": observatory_geo}
ANIMATIONS = {"observatory": observatory_animations}
SIZES = {"observatory": (OBSERVATORY_UV, OBSERVATORY_SIZES)}
