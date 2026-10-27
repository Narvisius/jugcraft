"""The Arcane Concordance's economy (roadmap step 29): every way one of its resources becomes another, every source that
time alone drives, and what representative installations take and give in a day.

Nothing here is generated into the game. tools/check_mod_data.py (check_economy) checks that:

- each figure below is still the one in the Java or the data it names (ANCHORS), so this model cannot drift from the game;
- no cycle of conversions, across every system, comes back with as much as it started with (gaining_cycles), the
  growing and composting of every Concordance crop included, at its natural cost and when a Verdant Spire hastens it;
- every source that time alone drives has a stated bound a day;
- the table in docs/features/arcane-concordance-economy.md is the one table() renders.

Run it to print the table: python3 tools/concordance_economy.py
"""
import json
from pathlib import Path

import concordance_rituals as rituals

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src" / "main" / "resources" / "data" / "jugcraft"
JAVA = ROOT / "src" / "main" / "java" / "io" / "github" / "jimbozoomer" / "jugcraft" / "concordance"

DAY = 24_000            # ticks (wonder/Spires.DAY)
HOUR = 72_000           # ticks in a real hour, at 20 ticks a second

# Every resource the Concordance converts, in its own units.
RESOURCES = {
    "focus": "Focus",
    "radiance": "Radiance (measures)",
    "ley": "Ley Charge",
    "verdance": "Verdance",
    "nutrient": "nutrients (in a Verdant Bed)",
    "vitae": "Vitae",
    "je": "Jugcraft Energy (JE)",
    "relic_charge": "relic charge",
    "bone_meal": "bone meal",
    "rooted_thallite_foundation": "Rooted Thallite Foundations (Root and Rivet's; nothing turns one back)",
    "crucible_heat": "a crucible's heat (a tick held at a Crucible Stand's target)",
    "astral_resonance": "Astral Resonance (in an astrolabe; observatories gather it, once an occurrence)",
    "recalled_attunement": "a recalled attunement (2,400 ticks of a pattern's effect; nothing turns one back)",
}

# Every conversion: (id, from, amount, to, amount). The best case is used (a thriving Heart), so the audit is safe.
CONVERSIONS = [
    ("focus_to_radiance", "focus", 6, "radiance", 2),
    ("radiance_to_ley", "radiance", 3, "ley", 2),
    ("verdance_to_ley", "verdance", 3, "ley", 2),
    ("je_to_ley", "je", 1000, "ley", 1),
    ("heart_beat", "nutrient", 1, "verdance", 2),
    ("bone_meal_to_nutrients", "bone_meal", 1, "nutrient", 2),
    ("ley_to_relic_charge", "ley", 1, "relic_charge", 4),
    ("crimson_surge", "vitae", 6, "focus", 6),
    # Concordance Chunk C, C-05: a Crucible Stand pays JE for a crucible's heat, modelled at its cheapest band (warm), so
    # the audit is safe; heat goes nowhere else (an edge into a node with no way out).
    ("je_to_heat", "je", 8, "crucible_heat", 1),
    # Concordance Chunk D, D-05 (D-S1): Star-reckoning spends resonance on one recalled attunement, modelled at its cheapest
    # recall (an edge into a node with no way out).
    ("star_reckoning", "astral_resonance", 6, "recalled_attunement", 1),
]

# Where each figure lives: (path under concordance/ or data:path, text that must be there).
ANCHORS = {
    "focus_to_radiance": [("data:concordance/conversion/focus_to_radiance.json", None)],
    "radiance_to_ley": [("data:concordance/conversion/radiance_to_ley.json", None)],
    "verdance_to_ley": [("data:concordance/conversion/verdance_to_ley.json", None)],
    "je_to_ley": [("LeyPylonBlockEntity.java", "JE_PER_LEY = 1000;"), ("LeyPylonBlockEntity.java", "JE_RATE = 64;")],
    "heart_beat": [("data:concordance/organism/verdant_heart.json", None),
                   ("garden/VerdantHeartBlockEntity.java", "BEAT_TICKS = 200;")],
    "bone_meal_to_nutrients": [("garden/Garden.java", "BONE_MEAL_NUTRIENTS = 2;")],
    "ley_to_relic_charge": [("reliquary/Reliquary.java", "CHARGE_PER_LEY = 4;"),
                            ("reliquary/Reliquary.java", "RECHARGE_PER_SECOND = 8;")],
    "crimson_surge": [("vigil/Vigil.java", "SURGE_VITAE = 6;"), ("vigil/Vigil.java", "SURGE_FOCUS = 6;"),
                      ("vigil/Vigil.java", "SURGE_COOLDOWN = 1200;")],
    "hastening": [("garden/OrganismCropBlock.java", "HASTENED_COST = 1;"),
                  ("garden/OrganismCropBlock.java", "if (!hastened && definition.fix() > 0) {"),
                  ("spire/ConcordSpire.java", "organism.hasten(level, at, crop, random)")],
    "burning": [("KindledLanternItem.java", "BURN_TICKS = 400;"), ("LumenSconceBlockEntity.java", "BURN_TICKS = 1200;"),
                ("LanternCharge.java", "Math.ceilDiv(now - since, (long) KindledLanternItem.BURN_TICKS)"),
                ("LumenSconceBlockEntity.java", "Math.ceilDiv(now - since, (long) BURN_TICKS)"),
                ("KindledLanternItem.java", "charge(stack).kept(now, true)")],
    "gleaner": [("garden/GleanerBlockEntity.java", "GLEAN_TICKS = 40;"), ("garden/GleanerBlockEntity.java", "COST = 1;")],
    "spire": [("data:concordance/wonder/concord_spire.json", None)],
    "root_and_rivet": [("data:concordance/ritual/root_and_rivet.json", None), ("ritual/RitualMachine.java", "STEP_TICKS = 40;"),
                       ("rules/FocusPool.java", "REGEN_TICKS = 40;")],
    "je_to_heat": [("CrucibleStandBlockEntity.java", "JE_WARM = 8;"), ("CrucibleStandBlockEntity.java", "JE_HOT = 16;"),
                   ("CrucibleStandBlockEntity.java", "JE_SEARING = 32;"),
                   ("CrucibleStandBlockEntity.java", "new SimpleEnergyStorage(CAPACITY, INTAKE, 0, this::setChanged)")],
    "star_reckoning": [("sky/AstrolabeItem.java", 'return "recall:" + pattern.id();'),
                       ("sky/AstrolabeItem.java", "setCharge(astrolabe, held - pattern.recallCost());"),
                       ("celestial/Calendar.java", "RECALL_TICKS = 2400;"),
                       ("spirits/StarReckoning.java", "AstrolabeItem.recall(player, level, astrolabe, pattern, time, gameTime)"),
                       ("data:concordance/pact/stolas.json", None)],
}

# Concordance Chunk D, D-05: Stolas's terms as concordance/pact/stolas.json writes them (days), and the recall's figures.
RECKONING_QUOTA = 1
RECALL_TICKS = 2_400  # celestial/Calendar.RECALL_TICKS
RECALL_COSTS = (6, 16)  # the cheapest and dearest recall (concordance/pattern/*.json attunement.recall)
STOLAS_UPKEEP = {"count": 1, "period_days": 7, "grace_days": 1, "cap": 4}
STOLAS_OFFERINGS = ["jugcraft:stolas_starflower", "jugcraft:ravenquill_lupine", "jugcraft:amethyst_mourningbell",
                    "minecraft:amethyst_shard"]

# The figures the anchors must show (checked against the data files by check_economy).
HASTENED_COST = 1
HEART_BEAT_TICKS = 200
GLEAN_TICKS = 40
GLEANER_COST = 1
JE_RATE = 64
RECHARGE_PER_SECOND = 8
SURGE_COOLDOWN = 1200
LANTERN_BURN_TICKS = 400
SCONCE_BURN_TICKS = 1200
FOCUS_MAX = 20
VITAE_AN_HOUR = 60     # check_crimson's bound on what an hour of offerings can give
STAND_JE = {"warm": 8, "hot": 16, "searing": 32}  # a Crucible Stand's JE a tick (CrucibleStandBlockEntity.JE_*)
STEP_TICKS = 40         # a ritual's step (ritual/RitualMachine.STEP_TICKS)
FOCUS_REGEN_TICKS = 40  # a player's Focus comes back one point this often (rules/FocusPool.REGEN_TICKS)

# Rituals that make a thing nothing turns back into a resource, modelled from Concordance Chunk C on (C-04; plan 06:144;
# the Adept's Attunement's wand is an instrument, excluded and never converted): what one completed run takes, the Ley
# Charge of every channel and each participant's Focus, for the one thing it makes. Each is an edge into a node with no
# way out, so it closes no cycle today; it is in the audit so that a later conversion out of that thing is weighed with
# it. check_economy holds each figure to its ritual's data; the offerings are worded from it (offerings()).
RITUAL_SINKS = {
    "root_and_rivet": {"rite": "Root and Rivet", "makes": "rooted_thallite_foundation", "name": "Rooted Thallite Foundations",
                       "ley": 32, "focus": 6, "ticks": 160,
                       "consequence": "none; a foundation never becomes Ley Charge, Focus or a plate again"},
}


def offerings(key):
    """A ritual's offerings in words, from tools/concordance_rituals.py RITUALS, as its JEI page words them."""
    said = [f"{o['count']} {rituals._item_name(o['item'])}" for o in rituals.RITUALS[key]["offerings"]]
    return ", ".join(said[:-1]) + f" and {said[-1]}" if len(said) > 1 else said[0]


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def organisms():
    return {path.stem: load(path) for path in sorted((DATA / "concordance" / "organism").glob("*.json"))}


def mulch_quarters():
    """Each item's mulch value in quarters of a nutrient, from #jugcraft:mulch/* (the Mulch Maw's food)."""
    quarters = {"quarter": 1, "half": 2, "three_quarters": 3, "whole": 4}
    values = {}
    for name, amount in quarters.items():
        path = DATA / "tags" / "item" / "mulch" / f"{name}.json"
        if path.is_file():
            for value in load(path).get("values", []):
                values[value] = amount
    return values


def crop_loops():
    """
    Growing a Concordance crop back after a harvest and composting what it gave, as nutrient to nutrient: at its own
    cost by time, and at least HASTENED_COST a step when a Verdant Spire hastens it (a hastened step fixes nothing).
    A fixer grown by time costs nothing and makes nutrients: a source, bounded by time (SOURCES), not a conversion.
    """
    mulch = mulch_quarters()
    chaff = mulch.get("jugcraft:verdant_chaff", 0)
    loops = []
    for key, entry in organisms().items():
        if entry.get("role") != "crop":
            continue
        back = entry.get("produce", 0) * mulch.get(entry.get("item"), 0) + entry.get("chaff", 0) * chaff
        steps = 3 - entry.get("replant", 0)
        for how, cost in (("grown", entry.get("cost", 0)), ("hastened", max(HASTENED_COST, entry.get("cost", 0)))):
            if cost > 0:
                loops.append((f"{key}_{how}", back, steps * cost * 4))
    return loops


def edges():
    """Every conversion as a ratio from one resource to another (the best of any two that link the same pair)."""
    ratios = {}
    for _, source, amount, result, made in CONVERSIONS:
        ratios[source, result] = max(ratios.get((source, result), 0.0), made / amount)
    for sink in RITUAL_SINKS.values():
        for resource in ("ley", "focus"):
            ratios[resource, sink["makes"]] = max(ratios.get((resource, sink["makes"]), 0.0), 1 / sink[resource])
    for _, back, spent in crop_loops():
        ratios["nutrient", "nutrient"] = max(ratios.get(("nutrient", "nutrient"), 0.0), back / spent)
    return ratios


def gaining_cycles(ratios):
    """The resources from which some cycle of conversions comes back with at least as much (none, in a sound economy)."""
    nodes = sorted({n for edge in ratios for n in edge})
    best = {(a, b): ratios.get((a, b), 0.0) for a in nodes for b in nodes}
    for k in nodes:
        for a in nodes:
            for b in nodes:
                best[a, b] = max(best[a, b], best[a, k] * best[k, b])
    return [n for n in nodes if best[n, n] >= 1 - 1e-9]


def spires():
    pulse = load(DATA / "concordance" / "wonder" / "concord_spire.json")["pulse"]
    configurations = {path.stem: load(path) for path in sorted((DATA / "concordance" / "wonder_configuration").glob("*.json"))}
    return pulse, configurations


# Every source that time alone drives (it takes no resource that is counted above), with its bound a day.
def sources():
    pulse, configurations = spires()
    pulses = DAY // pulse
    lantern, star, verdant = (configurations[k]["field"]["count"] for k in ("lantern_spire", "star_spire", "verdant_spire"))
    return [
        ("A Verdant Spire's field", f"at most {verdant * pulses:,} crop steps a day ({verdant} a pulse, a pulse every {pulse} ticks); "
         f"a Concordance crop pays its bed at least {HASTENED_COST} nutrient a step and fixes none"),
        ("A Star Spire's field", f"at most {pulses:,} Focus a day to each of {star} players a pulse, never above {FOCUS_MAX} held"),
        ("A Lantern Spire's field", f"at most {lantern * pulses:,} Kindled lights a day ({lantern} a pulse)"),
        ("Mendvetch (a fixer) grown by time", "1 nutrient a growth step, at a crop's natural pace (random ticks); "
         "never when hastened"),
        ("Offerings (health given as Vitae)", f"at most {VITAE_AN_HOUR} Vitae an hour, whatever heals the giver"),
        ("A Crimson Surge", f"at most {DAY // SURGE_COOLDOWN} a day (one every {SURGE_COOLDOWN} ticks)"),
    ]


def installations():
    """Representative installations a day: (name, inputs, outputs, bound, consequence)."""
    pulse, configurations = spires()
    pulses = DAY // pulse
    beats = DAY // HEART_BEAT_TICKS
    rows = [
        ("Verdant Heart on a thriving bed, beside a Ley Pylon",
         f"{beats} nutrients ({beats // 2} bone meal)", f"{beats * 2} Verdance, poured as {beats * 2 * 2 // 3} Ley Charge",
         f"one beat every {HEART_BEAT_TICKS} ticks", "the bed's nutrients fall; it stops on a starved bed"),
        ("Ley Pylon on Jugcraft Energy", f"{JE_RATE * DAY:,} JE", f"{JE_RATE * DAY // 1000:,} Ley Charge",
         f"{JE_RATE} JE a tick", "none; Ley Charge never becomes JE again"),
        ("Crucible Stand on Jugcraft Energy, heating searing all day", f"{STAND_JE['searing'] * DAY:,} JE",
         f"{DAY:,} ticks of one crucible's heat, no more",
         f"{STAND_JE['searing']} JE a tick searing, {STAND_JE['hot']} hot, {STAND_JE['warm']} warm; nothing while its crucible "
         "waits for anything but heat, is held, or is hotter than it aims",
         "none; heat is never stored, carried or turned back into JE, Ley Charge or anything else"),
        ("Gleaner on Concordance crops", f"{DAY // GLEAN_TICKS} Verdance", f"{DAY // GLEAN_TICKS} harvests, of ripe crops only",
         f"one every {GLEAN_TICKS} ticks", "each crop falls back to step 1 and must regrow at its cost"),
        ("Kindled Lantern, lit", f"{DAY // LANTERN_BURN_TICKS} Radiance", "light at its carrier",
         f"a measure every {LANTERN_BURN_TICKS} ticks; a measure begun is spent", "light"),
        ("Lumen Sconce", f"{DAY // SCONCE_BURN_TICKS} Radiance", "light 15", f"a measure every {SCONCE_BURN_TICKS} ticks; "
         "a measure begun is spent", "light"),
        ("Reliquary Shrine recharging", f"{RECHARGE_PER_SECOND * DAY // 20 // 4:,} Ley Charge", f"{RECHARGE_PER_SECOND * DAY // 20:,} relic charge",
         f"{RECHARGE_PER_SECOND} charge a second", "none; relic charge never becomes Ley Charge again"),
        ("Crimson Surge", f"{DAY // SURGE_COOLDOWN * 6} Vitae", f"{DAY // SURGE_COOLDOWN * 6} Focus",
         f"one every {SURGE_COOLDOWN} ticks", "health given (offering exhaustion)"),
    ]
    rows.append(("Stolas's pact, kept and served every day",
                 f"{STOLAS_UPKEEP['count']} offering every {STOLAS_UPKEEP['period_days']} game days (a Styx flower, which one bone "
                 f"meal grows back, or an amethyst shard), and {RECALL_COSTS[0]} to {RECALL_COSTS[1]} Astral Resonance a recall",
                 f"{RECKONING_QUOTA} recalled attunement a game day, {RECALL_TICKS:,} ticks long",
                 f"{RECKONING_QUOTA} a game day a pact; one recall an occurrence of each pattern, shared with a master's; at most "
                 f"{STOLAS_UPKEEP['cap']} offerings held ahead",
                 "none; an offering paid becomes nothing, and no resonance is made"))
    for key, sink in RITUAL_SINKS.items():
        runs = DAY // sink["ticks"]
        rows.append((f"{sink['rite']}, worked again and again at one Lesser Circle",
                     f"{runs * sink['ley']:,} Ley Charge, {runs * sink['focus']:,} Focus, and {offerings(key)} a run",
                     f"{runs} {sink['name']}", f"one run every {sink['ticks']} ticks; one player's Focus, back 1 every "
                     f"{FOCUS_REGEN_TICKS} ticks, pays for {DAY // FOCUS_REGEN_TICKS // sink['focus']} runs",
                     sink["consequence"]))
    for key, label in (("verdant_spire", "Verdant Spire"), ("star_spire", "Star Spire"), ("lantern_spire", "Lantern Spire")):
        entry = configurations[key]
        upkeep = entry["upkeep"]
        item = upkeep["item"].split(":")[1].replace("_", " ")
        item = item + "s" if upkeep["count"] > 1 and item.endswith("sac") else item
        field = entry["field"]
        made = {"growth": f"{field['count'] * pulses:,} crop steps", "focus": f"{pulses:,} Focus a player",
                "illumination": f"{field['count'] * pulses:,} lights"}[field["kind"]]
        rows.append((f"{label}, kept", f"{upkeep['count']} {item} and {upkeep['ley']} Ley Charge", made,
                     f"{field['count']} a pulse, a pulse every {pulse} ticks",
                     {"growth": "Concordance crops pay their beds", "focus": "hostile creatures revealed",
                      "illumination": "no hostile spawns in its light"}[field["kind"]]))
    return rows


def table():
    lines = ["| Installation, a day (24,000 ticks) | Takes at most | Gives at most | Bound | Consequence |", "|---|---|---|---|---|"]
    for row in installations():
        lines.append("| " + " | ".join(row) + " |")
    lines.append("")
    lines.append("| Source driven by time | Bound |")
    lines.append("|---|---|")
    for name, bound in sources():
        lines.append(f"| {name} | {bound} |")
    lines.append("")
    lines.append("| Crop loop (regrow, then compost the harvest) | Gives back (quarters) | Costs (quarters) |")
    lines.append("|---|---|---|")
    for name, back, spent in crop_loops():
        lines.append(f"| {name.replace('_', ', ')} | {back} | {spent} |")
    return "\n".join(lines)


if __name__ == "__main__":
    print(table())
    print()
    print("gaining cycles:", gaining_cycles(edges()) or "none")
