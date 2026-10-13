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
}

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
