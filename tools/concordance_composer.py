"""Chunk A (A-02, A-03): the Spell Composer, the screen that writes spells without commands.

The composer is a screen over the same service as /jugcraft concordance compose (concordance/Composing.java): the
server sends the words a player has learned and checks everything they write, in the command's own words; the screens
only edit a draft (concordance/compose/Draft.java). This module holds the screens' words; tools/check_mod_data.py
(check_composer) checks the Java and Kotlin against them.
"""

MOD = "jugcraft"

# The palette's rows, by grammar slot (compose/Slot.java ids).
SLOTS = {
    "delivery": "Deliveries",
    "selection": "Selections",
    "operation": "Operations",
    "modifier": "Modifiers",
    "termination": "Endings",
}

# The client: the key, the journal's button and the composer's screens.
CLIENT = {
    "key.jugcraft.concordance_composer": "Open the Spell Composer",
    "screen.jugcraft.journal.compose": "Compose",
    "screen.jugcraft.composer.title": "Spell Composer",
    "screen.jugcraft.composer.unavailable": "This server cannot compose spells",
    "screen.jugcraft.composer.opening": "Opening the composer...",
    "screen.jugcraft.composer.instrument": "%s: %s of %s capacity used; at most %s targets, %s work and %s branches",
    "screen.jugcraft.composer.inscribed": "Inscribed now: %s",
    "screen.jugcraft.composer.text": "Spell, as written",
    "screen.jugcraft.composer.hint": "Type a spell, or choose words below",
    "screen.jugcraft.composer.typed": "Written as text: the check below says how it reads",
    "screen.jugcraft.composer.empty": "Choose a delivery, a selection and an operation below",
    "screen.jugcraft.composer.spell": "Spell:",
    "screen.jugcraft.composer.then": "Then:",
    "screen.jugcraft.composer.earlier": "Move earlier",
    "screen.jugcraft.composer.later": "Move later",
    "screen.jugcraft.composer.remove": "Remove",
    "screen.jugcraft.composer.unjoin": "Remove modifier",
    "screen.jugcraft.composer.branch": "Add a branch",
    "screen.jugcraft.composer.unbranch": "Remove the last branch",
    "screen.jugcraft.composer.undo": "Undo",
    "screen.jugcraft.composer.restart": "Start again",
    "screen.jugcraft.composer.load": "Load inscribed",
    "screen.jugcraft.composer.locked": "%s more words are yet to be learned",
    "screen.jugcraft.composer.word_cost": "(capacity %s, Focus %s)",
    "screen.jugcraft.composer.checking": "Checking...",
    "screen.jugcraft.composer.writing": "Writing on the instrument...",
    "screen.jugcraft.composer.inscribe": "Inscribe",
    "screen.jugcraft.composer.wipe": "Wipe inscription",
    "screen.jugcraft.composer.help": "Choose words below, or type the spell. Tab and the arrow keys move between controls, "
                                     "and a word of your spell reached with them is chosen: Alt+Left and Alt+Right move "
                                     "it, Backspace removes it, and a modifier joins it. Ctrl+Z undoes. A word marked ! "
                                     "has a problem; the chosen word is in brackets. Inscribe writes a checked spell on "
                                     "your instrument. Esc goes back.",
    "screen.jugcraft.composer.help_workspace": "Choose words below, or type the spell. Tab moves between controls; "
                                               "Alt+Left and Alt+Right move the chosen word, Backspace removes it, Ctrl+Z "
                                               "undoes. A word marked ! has a problem; the chosen word is in brackets. "
                                               "Esc goes back.",
}


def lang_entries(lang):
    for key, name in SLOTS.items():
        lang[f"screen.{MOD}.composer.slot.{key}"] = name
    lang.update(CLIENT)
