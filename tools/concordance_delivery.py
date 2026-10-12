"""The Arcane Concordance's library integration matrix (roadmap step 32): docs/features/arcane-concordance-delivery.md.

One row for every library in distribution/frameworks.lock.json. A row covers what the Concordance actually does with
the library, what happens without it, the game tests that exercise it, and what is still limited. The version, side and
whether Jugcraft needs it come from the lock, never from here. The build (build.gradle) declares every library the lock
marks as required in fabric.mod.json's `depends`, so Fabric Loader refuses to start Jugcraft without one, and every
optional one in `suggests`.

Nothing here is generated into the game. tools/check_mod_data.py (check_delivery) checks that:

- every library in the lock has exactly one row, and every row names a library in the lock;
- every game test a row names exists (a method, or a whole client test class);
- a library whose row says it is not used really is not referred to from Jugcraft's code;
- the record's matrix is the one table() renders.

Run it to print the matrix: python3 tools/concordance_delivery.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / "distribution" / "frameworks.lock.json"
GAMETEST = ROOT / "src" / "gametest" / "java"
SOURCES = [ROOT / "src" / "main" / "java", ROOT / "src" / "client" / "java", ROOT / "src" / "client" / "kotlin"]

# key in the lock: (what the Concordance does with it, what happens without it, the game tests, what is still limited).
# Tests name a game test method, or a client game test class (whose single test is its runTest).
MATRIX = {
    "fabric-api": (
        "Attachments for every player record, and each wearer's worn trinkets choice, sent to everyone who sees them; item "
        "components; the rules' reload listener; menus; commands; networking (the journal and its request, signs, the "
        "Vigil's gesture, the worn trinkets choice); key mappings; the Focus HUD line; block-entity load and unload "
        "(the circle index); the block-break and attack questions asked for the person behind a change, with a "
        "FakePlayer standing in for an absent owner when a server allows it; the Transfer API for the crucible's water "
        "and every courier pickup and delivery",
        "required",
        ["theJournalSurvivesTheTrip", "aSignReachesTheClientUnchanged", "aRequestIsFetchedAndDelivered",
         "lightGoesOnlyWhereItsCasterCouldBuild", "theCircleIndexLooksOnlyInReach", "eachWearerChoosesWhetherTheirTrinketsShow"],
        "No pipe carries Concordance essences; a real protection mod has not been tried (a stand-in listener answers "
        "the two questions in the tests)"),
    "geckolib": (
        "Animated blocks and creatures driven by the status the server sends: Circle Anchor, Alembic Crucible (with "
        "its liquid at its volume), Verdant Heart, Mulch Maw, Gleaner, Orrery Observatory, Reliquary Shrine, Spire "
        "Heart, Oneiric Censer; Hearthling, Gathering Shade, Clockwork Porter and dream wisps; the two fire sets "
        "worn as the owner's armour model, each in its own texture (Jugcraft's first GeckoLib armour)",
        "required",
        ["ConcordancePresentationClientGameTests", "aPorterShowsOnceWhatItLacks", "ConcordanceEmberGearClientGameTests"],
        "Seen only in CI's small screenshot previews; frame cost of a large installation not measured; the sets' glint "
        "is a Jugcraft layer, and vanilla trims are not drawn on them"),
    "player-animation-library": (
        "Original cast and release gestures for the invocations; the circle participants' channelling gesture; the "
        "Vigil's offering gesture, sent by the server when an offering is made",
        "required",
        [],
        "No test looks at a gesture; skipped for players whose arms ArmsMotion poses"),
    "modonomicon": (
        "The Arcane Concordance codex: categories for every tradition, pages generated from the rules (components, "
        "the calendar, the Binding reasons, the spell bar's keys), entries unlocked by research advancements",
        "required",
        ["codexLoadsWithoutErrors", "metResearchSaysWhatComesNext"],
        "The codex has been read only as data; nobody has read it in a client"),
    "smartbrainlib": (
        "The workers' brains: a nearby-players sensor and look and walk behaviours driven by the walk target each "
        "worker's own decision sets; bonds, agreements, bodies and loads stay Jugcraft's records",
        "required",
        ["aPorterSaysWhyItStops", "aSpiritKeepsToItsAgreement", "aFamiliarMendsByItsBond"],
        "Pathing over long distances and through many loaded workers not profiled"),
    "fusion-connected-textures": (
        "Warding Stones and Verdant Beds join their neighbours, through a built-in resource pack registered only when "
        "Fusion is installed",
        "plain cubes",
        [],
        "Never seen in a client"),
    "lambdynamiclights": (
        "A lit Kindled Lantern, a charged Astrolabe and the Wardlight Lantern give light in hand (JSON keyed on items "
        "and their components; client light only)",
        "no light in hand; world light is unchanged",
        [],
        "Never seen in a client; tools/check_mod_data.py checks the JSON's components exist"),
    "spell-engine": (
        "Casting: the cast timeline, targeting, cooldowns, HUD and sounds; a container source offering a player's "
        "learned invocations while an instrument is held; the casting gate; custom impacts into the shared effect "
        "boundary; the cost event; spell assignments that keep Jugcraft weapons from casting",
        "required",
        ["kindleCastsThroughSpellEngine", "inscribeAndCastThroughSpellEngine", "cooldownIsNeverShorterThanTheComposition",
         "ConcordanceJourneyClientGameTests"],
        "Composed deliveries are Jugcraft's own bounded traces, not Spell Engine projectiles; which invocation sits on "
        "the use key is Spell Engine's order"),
    "spell-power": (
        "Schools name each Principle's damage type, so resistances apply once; the Lance scales with arcane Spell "
        "Power, Cinderbolt and Hearthflare with fire; Resonant Rings can carry arcane Spell Power, the Ember foci and "
        "the two fire sets fire; Spell Power's attribute enchantments are refused on the sets",
        "required",
        ["lanceStrikesAndScalesWithSpellPower", "everyStatisticIsARealAttribute", "cinderboltBurnsAndGrowsWithFireSpellPower",
         "theRegaliaRaisesTheFireToItsCeiling", "spellPowerEnchantmentsAreRefusedOnTheSet"],
        "Concordance damage adds Spell Power's points only, not its critical hits"),
    "trinkets-updated": (
        "Resonant Rings in the ring slot and the Hearthstone in the necklace slot, both given by data; the owner's "
        "Spell Focus and Bracelet slots, ported from their Curios data, for the Ember foci and the Fire Bangle; a ring's "
        "and a focus's modifiers through Trinkets' callback; relics and the bangle read from the slots on the server; "
        "Wayfaring's Belt, Charm and Feet slots, with named callback modifiers, a slot-count attribute and Relic Lore's "
        "canEquip; the Leather Belt and Amphibian Boot drawn on the wearer by Trinkets' data-driven renderer (a render "
        "definition and block models from the owner's worn sheets), through Jugcraft's own render element, which hides "
        "them under armour and on a wearer who has chosen to hide them",
        "required",
        ["relicsAreFoundInTrinketSlots", "gemsRunesAndBondsKeepTheirRules", "theFociGiveFireSpellPowerThroughTrinkets",
         "theBangleLeavesAHearthbindersBlowSmouldering", "ConcordanceEmberGearClientGameTests",
         "theTrinketsAreMadeAndWornAsDesigned", "ConcordanceWayfaringClientGameTests"],
        "Cosmetic slots count as worn for show only; of the worn things only the belt and boot are drawn on the body, "
        "in third person only, and seen so far only in CI's client screenshots"),
    "cloth-config": (
        "The Concordance settings screen: the Focus line, reduced motion, exact values, the simple journal, visual "
        "intensity and whether your worn trinkets are shown",
        "required",
        [],
        "No test opens the screen; the settings it writes are read by client code only, but for the worn trinkets "
        "choice, which the client sends to the server"),
    "jade": (
        "Readouts from server snapshots: bench study, lantern Radiance, sconce charge, circle phase and faults, pylon "
        "charge, crucible state, beds and crops, living devices, observatory, workers, courier posts, shrines",
        "nothing shown on looking; the same facts come from the journal, the commands and each device's own messages",
        ["benchStudyUnderstandsFirstLight"],
        "Server tests run without Jade in CI's optional-absent job; its overlay has not been seen in a client"),
    "jei": (
        "Recipe categories for the Lampwright's Bench, the Circle Anchor and the Resonant Ring; alchemy ingredients' "
        "properties as information (never outcomes)",
        "no JEI pages; the codex's recipe pages remain",
        [],
        "Its pages have not been seen in a client"),
    "modmenu": (
        "Opens the Concordance settings screen from the mod list",
        "the Concordance settings key (unbound by default, in Controls) opens the same screen",
        [],
        "Not seen in a client"),
    "iris": (
        "Not used: nothing in the Concordance needs a shader",
        "nothing changes",
        [],
        "No shader pack has been tried with the Concordance"),
    "sodium": (
        "Not used",
        "nothing changes",
        [],
        "Not tried with the Concordance's renderers"),
    "fabric-language-kotlin": (
        "Runs the Kotlin of the journal's GuiLib workspace",
        "the plain journal screen",
        ["ConcordanceJournalClientGameTests"],
        "Only the workspace is Kotlin"),
    "guilib": (
        "The Concordance Journal's workspace, a tab for each section of the journal the server sends",
        "the plain journal screen, with the same journal",
        ["ConcordanceJournalClientGameTests"],
        "No composer, ritual-schematic, crucible or observatory screen is built; keyboard use not tried by a person"),
}

# Libraries the Concordance does not use: their mod ids must not appear in Jugcraft's code.
UNUSED = {"iris": ("net.irisshaders", "iris"), "sodium": ("net.caffeinemc", "sodium")}


def lock():
    return json.loads(LOCK.read_text(encoding="utf-8"))


def game_tests():
    """Every game test method, and every client game test class, in src/gametest."""
    names = set()
    for path in GAMETEST.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        names.update(re.findall(r"public void (\w+)\((?:GameTestHelper|ClientGameTestContext)", text))
        if "implements FabricClientGameTest" in text:
            names.add(path.stem)
    return names


def problems():
    found = []
    keys = [library["key"] for library in lock()["dependencies"]]
    for key in keys:
        if key not in MATRIX:
            found.append(f"delivery: the lock's {key} has no row in the matrix")
    for key in MATRIX:
        if key not in keys:
            found.append(f"delivery: the matrix's {key} is not in the lock")
    required = {library["key"]: bool(library.get("requiredByJugcraft")) for library in lock()["dependencies"]}
    for key, (_use, without, _named, _limits) in MATRIX.items():
        if key in required and (without == "required") != required[key]:
            found.append(f"delivery {key}: the matrix and the lock disagree on whether Jugcraft needs it")
    tests = game_tests()
    for key, (_use, _without, named, _limits) in MATRIX.items():
        for name in named:
            if name not in tests:
                found.append(f"delivery {key}: no game test {name}")
    code = "\n".join(path.read_text(encoding="utf-8") for base in SOURCES if base.exists()
                     for path in base.rglob("*") if path.suffix in (".java", ".kt"))
    for key, (package, mod_id) in UNUSED.items():
        if package in code or f'isModLoaded("{mod_id}")' in code:
            found.append(f"delivery {key}: the matrix says it is not used, but Jugcraft's code refers to it")
    return found


def table():
    lines = ["| Library | Version (lock) | Side | Jugcraft | What the Concordance does with it | Without it | Game tests | "
             "Still limited |", "|---|---|---|---|---|---|---|---|"]
    for library in lock()["dependencies"]:
        use, without, named, limits = MATRIX.get(library["key"], ("", "", [], ""))
        required = "required" if library.get("requiredByJugcraft") else "optional"
        if required == "required":
            without = "Jugcraft does not start (Fabric Loader names it as missing)"
        tests = ", ".join(f"`{name}`" for name in named) or "none"
        name = re.sub(r"[^\w\s().+\-]", "", library["name"]).strip()
        lines.append(f"| {name} | {library['modVersion']} | {library['side']} | {required} | {use} | {without} | "
                     f"{tests} | {limits} |")
    return lines


def guide_tables():
    """The player guide's generated tables (docs/ARCANE_CONCORDANCE_GUIDE.md): every research entry with how it begins, is
    understood and is mastered, in the words of its own advancements; and every stage with each route to it."""
    sys.path.insert(0, str(ROOT / "tools"))
    import concordance_progression as pg
    lang = json.loads((ROOT / "src" / "main" / "resources" / "assets" / "jugcraft" / "lang" / "en_us.json").read_text(encoding="utf-8"))
    folder = ROOT / "src" / "main" / "resources" / "data" / "jugcraft" / "concordance" / "research"
    research = {path.stem: json.loads(path.read_text(encoding="utf-8")) for path in sorted(folder.glob("*.json"))}
    order = sorted(research, key=lambda key: (pg.STAGES.get(research[key]["stage"], {}).get("order", 9), key))

    def said(key, state):
        return lang.get(f"advancements.jugcraft.concordance_{key}_{state}.description", "")
    lines = ["| Research | Tradition | Begin | Understand | Master |", "|---|---|---|---|---|"]
    for key in order:
        tradition = research[key]["tradition"].replace("_", " ").title()
        lines.append(f"| {lang.get(f'research.jugcraft.{key}', key)} | {tradition} | {said(key, 'encountered')} | "
                     f"{said(key, 'understood')} | {said(key, 'mastered')} |")
    lines += ["", "| Stage | Reached by any one of |", "|---|---|"]
    for key, info in sorted(pg.STAGES.items(), key=lambda item: item[1]["order"]):
        routes = "; ".join(pg.ROUTE_TEXT[(key, route["id"])] for route in info["routes"])
        lines.append(f"| {info['name']} | {routes} |")
    return lines


if __name__ == "__main__":
    print("\n".join(table()))
    issues = problems()
    print("\n".join(issues) if issues else "delivery: the matrix holds")
    sys.exit(1 if issues else 0)
