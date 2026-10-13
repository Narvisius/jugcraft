"""Roadmap step 26: the Concordance Journal (concordance/journal/Journal.java and the client's journal screens).

The journal shows, in one place, everything the server knows about a player's Concordance work and what stands in its
way: research (each entry's state, what the next state asks in words and, as exact figures, how far each way to it has
come), the stage and every route onward, and each tradition's own report. Every report is the one its chat command
prints; the journal adds no rules of its own. This module holds its words, the client's words and its codex page;
tools/check_mod_data.py (check_journal) checks the Java against them.
"""

MOD = "jugcraft"

# The journal's sections, in the order Journal.register() contributes them, with their titles.
SECTIONS = {
    "overview": "Overview",
    "research": "Research",
    "stage": "Stage",
    "vitae": "Vitae",
    "sky": "The Sky",
    "workers": "Workers",
    "logistics": "Deliveries",
    "relics": "Relics",
    "hexes": "Curses and Wards",
    "conclave": "The Conclave",
    "spires": "Spires",
    "assay": "Prima Materia",
}

STATES = {"none": "unknown", "encountered": "encountered", "observed": "observed", "understood": "understood",
          "mastered": "mastered"}

# How far each way to the next research state has come (EvidenceRule.Kind ids): have, then need.
RULES = {
    "examine": "examined %s of %s",
    "study": "studied %s of %s",
    "invoke": "cast %s of %s",
    "notes": "read %s of %s notes",
    "practice": "practised %s of %s",
}

JOURNAL = {
    "overview.focus": "Focus: %s of %s",
    "overview.stage": "Stage: %s",
    "overview.no_stage": "Stage: not yet begun (examine something that holds its own light)",
    "overview.research": "Research: %s entries met, %s mastered",
    "overview.research_total": "(of %s)",
    "research.next": "%s: %s. Next, %s: %s",
    "research.complete": "%s: %s, all there is to learn",
    "research.needs": "%s: %s. Before more, %s must be %s",
    "research.has": "(it is %s)",
    "research.unknown": "%s: not in the loaded rules",
    "research.unknown_count": "%s more entries not yet encountered",
    "or": "; or ",
    "assay.balance": "Prima Materia held: %s grains",
    "assay.cap": "(at most %s)",
    "unreadable": "This part of the journal could not be read; the server's log says why",
}

# The client: the key, the screens and the settings.
CLIENT = {
    "key.jugcraft.concordance_journal": "Concordance Journal",
    "screen.jugcraft.journal.title": "Concordance Journal",
    "screen.jugcraft.journal.loading": "Reading your journal...",
    "screen.jugcraft.journal.empty": "Nothing to show yet",
    "screen.jugcraft.journal.refresh": "Refresh",
    "screen.jugcraft.journal.exact_on": "Exact values: shown",
    "screen.jugcraft.journal.exact_off": "Exact values: hidden",
    "screen.jugcraft.journal.help": "Left, Right or the section buttons: sections. Up, Down, Page Up, Page Down: scroll. "
                                    "R: refresh. X: exact values. Tab: next button. Esc: close.",
    "screen.jugcraft.journal.section": "Section %s of %s: %s",
    "screen.jugcraft.journal.missing_guilib": "The full workspace needs GuiLib; showing the simple journal",
    "screen.jugcraft.concordance.config.exact": "Show exact values",
    "screen.jugcraft.concordance.config.exact.tooltip": "The journal shows the exact figures (counts, limits) beside its "
                                                        "descriptions.",
    "screen.jugcraft.concordance.config.simple_journal": "Simple journal",
    "screen.jugcraft.concordance.config.simple_journal.tooltip": "Open the plain journal screen even when GuiLib is installed.",
}


def lang_entries(lang):
    for key, name in SECTIONS.items():
        lang[f"journal.{MOD}.section.{key}"] = name
    for key, name in STATES.items():
        lang[f"journal.{MOD}.state.{key}"] = name
    for key, text in RULES.items():
        lang[f"journal.{MOD}.rule.{key}"] = text
    for key, text in JOURNAL.items():
        lang[f"journal.{MOD}.{key}"] = text
    lang.update(CLIENT)


def codex():
    return {("foundations", "journal"): {
        "name": "The Concordance Journal", "x": 4, "y": 2, "icon": "minecraft:writable_book", "condition": None,
        "description": "Everything you know and everything in your way, in one place",
        "pages": [
            ("text", "The Journal",
             "Press **J** (Controls: Concordance Journal) to open your journal. It shows what the server knows of your "
             "work: your Focus and stage, every research entry you have met and what its next state asks, each route to "
             "the next stage and what it still needs, and, once you know their traditions, your Vitae, the sky's "
             "forecast, your workers, deliveries, relics, the curses and wards on you, your Conclave standing, your "
             "spires and your Prima Materia."),
            ("text", "Reading It",
             "Each line says in words where you stand and what to do next. **Exact values** (in the journal, or in the "
             "Concordance settings) add the figures behind them: how many of what each way needs, limits and caps. "
             "The same reports print in chat with **/jugcraft concordance status**, **stage**, **spire** and the "
             "other Concordance commands."),
            ("text", "Moving Around",
             "**Left**, **Right** or the section buttons change section; **Up**, **Down**, **Page Up** and "
             "**Page Down** scroll; **R** asks for the journal again; **X** shows or hides exact values; **Tab** "
             "moves between buttons; **Esc** closes it. With GuiLib installed the journal opens as a full workspace; without it, or with "
             "**Simple journal** chosen in the settings, it opens as a plain screen showing the same."),
        ]}}
